package com.chatspellcheck;

import java.awt.event.KeyEvent;
import java.util.Objects;
import javax.inject.Inject;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.VarClientStr;
import net.runelite.api.events.VarClientStrChanged;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.KeyListener;

/**
 * Intercepts Enter in the chatbox: if typos are present, consumes the keypress once and waits
 * for a second, unmodified Enter before letting the real keypress (and thus the send) through.
 * Never alters the outgoing message and never injects input of its own.
 */
class SendGuard implements KeyListener
{
	private final ChatSpellcheckConfig config;
	private final ChatInputTracker chatInputTracker;
	private final Client client;

	@Getter
	private volatile boolean pendingConfirmation;
	private volatile String pendingText;

	@Inject
	SendGuard(ChatSpellcheckConfig config, ChatInputTracker chatInputTracker, Client client)
	{
		this.config = config;
		this.chatInputTracker = chatInputTracker;
		this.client = client;
	}

	@Subscribe
	public void onVarClientStrChanged(VarClientStrChanged event)
	{
		if (event.getIndex() != VarClientStr.CHATBOX_TYPED_TEXT || !pendingConfirmation)
		{
			return;
		}

		if (!Objects.equals(client.getVarcStrValue(VarClientStr.CHATBOX_TYPED_TEXT), pendingText))
		{
			clearPending();
		}
	}

	@Override
	public void keyPressed(KeyEvent e)
	{
		if (e.getKeyCode() != KeyEvent.VK_ENTER || !config.blockOnTypos())
		{
			return;
		}

		String currentText = client.getVarcStrValue(VarClientStr.CHATBOX_TYPED_TEXT);

		if (pendingConfirmation && Objects.equals(currentText, pendingText))
		{
			// Second Enter with the text unchanged: let the real keypress through.
			clearPending();
			return;
		}

		if (!chatInputTracker.getFlaggedWords().isEmpty())
		{
			e.consume();
			pendingText = currentText;
			pendingConfirmation = true;
		}
	}

	@Override
	public void keyReleased(KeyEvent e)
	{
	}

	@Override
	public void keyTyped(KeyEvent e)
	{
	}

	void reset()
	{
		clearPending();
	}

	private void clearPending()
	{
		pendingConfirmation = false;
		pendingText = null;
	}
}
