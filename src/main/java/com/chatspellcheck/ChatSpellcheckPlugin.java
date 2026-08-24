package com.chatspellcheck;

import com.google.inject.Provides;
import java.util.concurrent.ScheduledExecutorService;
import javax.inject.Inject;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(
	name = "Chat Spellcheck",
	description = "Flags likely misspelled words as you type in the chatbox and warns before sending a message with typos",
	tags = {"chat", "spellcheck", "spelling", "typo"}
)
public class ChatSpellcheckPlugin extends Plugin
{
	@Inject
	private EventBus eventBus;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private KeyManager keyManager;

	@Inject
	private ScheduledExecutorService executor;

	@Inject
	private SpellcheckDictionary dictionary;

	@Inject
	private IgnoreListStore ignoreListStore;

	@Inject
	private ChatInputTracker chatInputTracker;

	@Inject
	private SpellcheckMenuManager menuManager;

	@Inject
	private SendGuard sendGuard;

	@Inject
	private ChatSpellcheckOverlay overlay;

	@Override
	protected void startUp()
	{
		ignoreListStore.load();
		executor.execute(dictionary::load);

		eventBus.register(chatInputTracker);
		eventBus.register(menuManager);
		eventBus.register(sendGuard);

		overlayManager.add(overlay);
		keyManager.registerKeyListener(sendGuard);
	}

	@Override
	protected void shutDown()
	{
		keyManager.unregisterKeyListener(sendGuard);
		overlayManager.remove(overlay);

		eventBus.unregister(sendGuard);
		eventBus.unregister(menuManager);
		eventBus.unregister(chatInputTracker);

		chatInputTracker.reset();
		sendGuard.reset();
	}

	@Provides
	ChatSpellcheckConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ChatSpellcheckConfig.class);
	}
}
