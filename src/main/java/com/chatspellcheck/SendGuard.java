package com.chatspellcheck;

import java.util.Objects;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ChatInput;
import net.runelite.client.events.ChatboxInput;
import net.runelite.client.events.PrivateMessageInput;

/**
 * Intercepts the chatbox send action: if typos are present, consumes the event once (blocking
 * the send, per RuneLite's own {@code ChatInputManager}) and waits for a second, unmodified send
 * attempt before letting it through. Never alters the outgoing message and never injects input -
 * {@link ChatInput#consume()} is RuneLite's own sanctioned veto mechanism for this, the same one
 * its built-in chat-command handling uses. Public/clan/etc. chat and private messages fire as two
 * separate event types ({@link ChatboxInput} and {@link PrivateMessageInput}), so both are handled.
 *
 * The private-message compose window closes on Enter even when the send is blocked (confirmed
 * in-game - the client resets the input regardless of {@code consume()}), so there's no persisted
 * text left for a "type it again unchanged" comparison there. {@link #pendingSince} lets the
 * overlay show a timed banner in that case instead of relying on a text match.
 */
@Slf4j
@Singleton
class SendGuard
{
	private final ChatSpellcheckConfig config;
	private final ChatInputTracker chatInputTracker;

	@Getter
	private volatile boolean pendingConfirmation;
	@Getter
	private volatile String pendingText;
	@Getter
	private volatile long pendingSince;

	@Inject
	SendGuard(ChatSpellcheckConfig config, ChatInputTracker chatInputTracker)
	{
		this.config = config;
		this.chatInputTracker = chatInputTracker;
	}

	@Subscribe
	public void onChatboxInput(ChatboxInput event)
	{
		guard(event, event.getValue());
	}

	@Subscribe
	public void onPrivateMessageInput(PrivateMessageInput event)
	{
		guard(event, event.getMessage());
	}

	private void guard(ChatInput event, String text)
	{
		log.debug("Chat Spellcheck [diag]: guard text='{}' blockOnTypos={} flagged={}",
			text, config.blockOnTypos(), chatInputTracker.getFlaggedWords());

		if (!config.blockOnTypos())
		{
			return;
		}

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
			pendingSince = System.currentTimeMillis();
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
