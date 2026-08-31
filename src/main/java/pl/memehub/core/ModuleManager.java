package pl.memehub.core;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.entity.Entity;
import pl.memehub.event.ClientTickEvent;
import pl.memehub.event.EventBus;
import pl.memehub.event.Subscribe;
import pl.memehub.modules.combat.AnchorAura;
import pl.memehub.modules.combat.AutoCrystal;
import pl.memehub.modules.combat.AutoTotem;
import pl.memehub.modules.combat.Criticals;
import pl.memehub.modules.combat.KillAura;
import pl.memehub.modules.combat.MaceSmashAssist;
import pl.memehub.modules.combat.Surround;
import pl.memehub.modules.combat.Triggerbot;
import pl.memehub.modules.combat.Velocity;
import pl.memehub.modules.combat.WindChargeSynergy;
import pl.memehub.modules.movement.AirJump;
import pl.memehub.modules.movement.NoFall;
import pl.memehub.modules.movement.Speed;
import pl.memehub.modules.movement.Sprint;
import pl.memehub.modules.render.Fullbright;
import pl.memehub.modules.render.HudModule;
import pl.memehub.modules.render.Watermark;
import pl.memehub.modules.utility.AutoRespawn;
import pl.memehub.modules.utility.AutoTool;
import pl.memehub.modules.utility.ClickGuiModule;
import pl.memehub.modules.utility.Panic;
import pl.memehub.modules.vehicle.CartBreaker;
import pl.memehub.modules.vehicle.CartPlacer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Rejestr wszystkich modulow oraz glowny "dyspozytor" zdarzen.
 *
 * <p>ModuleManager subskrybuje {@link ClientTickEvent} (wysylany przez mixin
 * {@code Minecraft#tick}) i na jego podstawie: obsluguje keybindy modulow,
 * a nastepnie przekazuje tick do kazdego wlaczonego modulu.
 */
public final class ModuleManager {
	public static final ModuleManager INSTANCE = new ModuleManager();

	private final List<Module> modules = new ArrayList<>();
	private final Map<String, Module> byName = new HashMap<>();
	private final Map<Class<? extends Module>, Module> byClass = new HashMap<>();

	/** Klawisze wcisniete w poprzednim ticku - do wykrywania krawedzi wcisniecia. */
	private final Set<Integer> lastKeys = new HashSet<>();

	private boolean initialized = false;

	private ModuleManager() {
	}

	/** Rejestruje wszystkie moduly i subskrypcje event bussa. Wywoluje sie raz, z klienta. */
	public void init() {
		if (initialized) {
			return;
		}
		initialized = true;

		// Combat
		register(new AutoCrystal());
		register(new AnchorAura());
		register(new AutoTotem());
		register(new KillAura());
		register(new Criticals());
		register(new Triggerbot());
		register(new MaceSmashAssist());
		register(new WindChargeSynergy());
		register(new Velocity());
		register(new Surround());

		// Cart PvP
		register(new CartPlacer());
		register(new CartBreaker());

		// Movement
		register(new Sprint());
		register(new NoFall());
		register(new Speed());
		register(new AirJump());

		// Render
		register(new Fullbright());
		register(new HudModule());
		register(new Watermark());

		// Utility
		register(new ClickGuiModule());
		register(new Panic());
		register(new AutoTool());
		register(new AutoRespawn());

		EventBus.INSTANCE.register(this);
	}

	private void register(Module module) {
		modules.add(module);
		byName.put(module.name().toLowerCase(), module);
		byClass.put(module.getClass(), module);
		// Kazdy modul jest obserwatorem event bussa (metody @Subscribe,
		// np. HUD render); obslugi onTick uzywaja only wlaczone moduly.
		EventBus.INSTANCE.register(module);
	}

	// ------------------------------------------------------------------
	// Dostep do modulow
	// ------------------------------------------------------------------

	public List<Module> getAll() {
		return modules;
	}

	public List<Module> getByCategory(Category category) {
		List<Module> result = new ArrayList<>();
		for (Module module : modules) {
			if (module.category() == category) {
				result.add(module);
			}
		}
		return result;
	}

	public Module get(String name) {
		return byName.get(name.toLowerCase());
	}

	@SuppressWarnings("unchecked")
	public <T extends Module> T get(Class<T> clazz) {
		return (T) byClass.get(clazz);
	}

	public boolean isEnabled(Class<? extends Module> clazz) {
		Module module = byClass.get(clazz);
		return module != null && module.isEnabled();
	}

	// ------------------------------------------------------------------
	// Glowny tick (subskrypcja event bussa)
	// ------------------------------------------------------------------

	@Subscribe
	public void onClientTick(ClientTickEvent.Pre event) {
		handleKeybinds();
		for (Module module : modules) {
			if (module.isEnabled()) {
				module.onTick();
			}
		}
	}

	/** Wykrywa wcisniecie klawiszy przypisanych do modulow (krawedz sygnalu). */
	private void handleKeybinds() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.getWindow() == null) {
			return;
		}
		long window = mc.getWindow().getWindow();
		Set<Integer> current = new HashSet<>();
		for (Module module : modules) {
			if (module.key == 0) {
				continue;
			}
			boolean down = InputConstants.isKeyDown(window, module.key);
			current.add(module.key);
			if (down && !lastKeys.contains(module.key)) {
				module.toggle();
			}
		}
		lastKeys.clear();
		lastKeys.addAll(current);
	}

	// ------------------------------------------------------------------
	// Hooki wywolywane z mixinow
	// ------------------------------------------------------------------

	/** Wywolywane przed atakiem (mixin MultiPlayerGameMode#attack). */
	public boolean onPreAttack(LocalPlayer player, Entity target) {
		boolean handled = false;
		for (Module module : modules) {
			if (module.isEnabled() && module.onPreAttack(player, target)) {
				handled = true;
			}
		}
		return handled;
	}

	/** Wywolywane przy kazdym wysylanym pakiecie (mixin Connection#send). */
	public void onPacketSend(Packet<?> packet) {
		for (Module module : modules) {
			if (module.isEnabled()) {
				module.onPacketSend(packet);
			}
		}
	}

	/** Wylacza wszystkie moduly oprocz podanego (uzywane przez Panic). */
	public void disableAllExcept(Module except) {
		for (Module module : modules) {
			if (module != except && module.isEnabled()) {
				module.setEnabled(false);
			}
		}
	}
}
