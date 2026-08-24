package com.chatspellcheck;

import java.util.Objects;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ChatboxInput;

/**
 * Intercepts the chatbox send action: if typos are present, consumes the event once (blocking
 * the send, per RuneLite's own {@code ChatInputManager}) and waits for a second, unmodified send
 * attempt before letting it through. Never alters the outgoing message and never injects input -
 * {@link ChatboxInput#consume()} is RuneLite's own sanctioned veto mechanism for this, the same
 * one its built-in chat-command handling uses.
 */
@Singleton
class SendGuard
{
	private final ChatSpellcheckConfig config;
	private final ChatInputTracker chatInputTracker;

	@Getter
	private volatile boolean pendingConfirmation;
	@Getter
	private volatile String pendingText;

	@Inject
	SendGuard(ChatSpellcheckConfig config, ChatInputTracker chatInputTracker)
	{
		this.config = config;
		this.chatInputTracker = chatInputTracker;
	}

	@Subscribe
	public void onChatboxInput(ChatboxInput event)
	{
		if (!config.blockOnTypos())
		{
			return;
		}

		String text = event.getValue();

		if (pendingConfirmation && Objects.equals(text, pendingText))
		{
			// Second send attempt with the text unchanged: let it through.
			clearPending();
			return;
		}

		if (!chatInputTracker.getFlaggedWords().isEmpty())
		{
			event.consume();
			pendingText = text;
			pendingConfirmation = true;
		}
		else
		{
			clearPending();
		}
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
