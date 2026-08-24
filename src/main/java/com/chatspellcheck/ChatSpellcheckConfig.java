package com.chatspellcheck;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup(IgnoreListStore.CONFIG_GROUP)
public interface ChatSpellcheckConfig extends Config
{
	@ConfigSection(
		name = "Appearance",
		description = "Colors and sizing for the underline and suggestion box.",
		position = 10
	)
	String appearanceSection = "appearance";

	@ConfigItem(
		keyName = "blockOnTypos",
		name = "Block send on typos",
		description = "Pressing Enter with likely typos present shows a confirmation instead of sending immediately.",
		position = 1
	)
	default boolean blockOnTypos()
	{
		return true;
	}

	@ConfigItem(
		keyName = "highlightMisspelledWords",
		name = "Highlight misspelled words",
		description = "Underlines likely misspelled words in the chatbox input and shows a suggestion.",
		position = 2
	)
	default boolean highlightMisspelledWords()
	{
		return true;
	}

	@ConfigItem(
		keyName = "suggestionTimeoutEnabled",
		name = "Suggestion auto-hide",
		description = "Hides the suggestion box after it's been sitting idle for the timeout below, so it doesn't linger indefinitely.",
		position = 3
	)
	default boolean suggestionTimeoutEnabled()
	{
		return true;
	}

	@Range(min = 1, max = 60)
	@ConfigItem(
		keyName = "suggestionTimeoutSeconds",
		name = "Suggestion timeout (seconds)",
		description = "How long the suggestion box stays visible after it stops changing, if auto-hide is on.",
		position = 4
	)
	default int suggestionTimeoutSeconds()
	{
		return 5;
	}

	@ConfigItem(
		keyName = IgnoreListStore.IGNORE_LIST_KEY,
		name = "Ignored words",
		description = "Words the spellchecker won't flag. Comma-separated - remove a word here to un-ignore it.",
		position = 5
	)
	default String ignoreList()
	{
		return "";
	}

	@Range(min = 8, max = 32)
	@ConfigItem(
		keyName = "suggestionFontSize",
		name = "Font size",
		description = "Text size for the suggestion box. The box grows or shrinks to fit.",
		position = 11,
		section = appearanceSection
	)
	default int suggestionFontSize()
	{
		return 16;
	}

	@Alpha
	@ConfigItem(
		keyName = "underlineColor",
		name = "Underline color",
		description = "Color of the underline drawn beneath misspelled words.",
		position = 12,
		section = appearanceSection
	)
	default Color underlineColor()
	{
		return new Color(255, 64, 64);
	}

	@Alpha
	@ConfigItem(
		keyName = "suggestionTypedColor",
		name = "Suggestion: typed text color",
		description = "Color of the part of the suggestion that matches what you've typed so far.",
		position = 13,
		section = appearanceSection
	)
	default Color suggestionTypedColor()
	{
		return new Color(140, 255, 140);
	}

	@Alpha
	@ConfigItem(
		keyName = "suggestionRemainingColor",
		name = "Suggestion: remaining text color",
		description = "Color of the part of the suggestion you haven't typed yet.",
		position = 14,
		section = appearanceSection
	)
	default Color suggestionRemainingColor()
	{
		return new Color(255, 220, 150);
	}

	@Alpha
	@ConfigItem(
		keyName = "suggestionBackgroundColor",
		name = "Suggestion background",
		description = "Fill color of the suggestion box.",
		position = 15,
		section = appearanceSection
	)
	default Color suggestionBackgroundColor()
	{
		return new Color(20, 20, 20, 235);
	}

	@Alpha
	@ConfigItem(
		keyName = "suggestionBorderColor",
		name = "Suggestion border",
		description = "Outline color of the suggestion box.",
		position = 16,
		section = appearanceSection
	)
	default Color suggestionBorderColor()
	{
		return new Color(255, 180, 50);
	}

	@Alpha
	@ConfigItem(
		keyName = "blockedBannerTextColor",
		name = "Blocked-message text",
		description = "Text color of the \"message blocked\" notice shown after a send is blocked for typos.",
		position = 17,
		section = appearanceSection
	)
	default Color blockedBannerTextColor()
	{
		return new Color(255, 220, 150);
	}

	@Alpha
	@ConfigItem(
		keyName = "blockedBannerBackgroundColor",
		name = "Blocked-message background",
		description = "Fill color of the \"message blocked\" notice.",
		position = 18,
		section = appearanceSection
	)
	default Color blockedBannerBackgroundColor()
	{
		return new Color(20, 20, 20, 235);
	}

	@Alpha
	@ConfigItem(
		keyName = "blockedBannerBorderColor",
		name = "Blocked-message border",
		description = "Outline color of the \"message blocked\" notice.",
		position = 19,
		section = appearanceSection
	)
	default Color blockedBannerBorderColor()
	{
		return new Color(220, 60, 60);
	}

	@Alpha
	@ConfigItem(
		keyName = "ignoreMenuColor",
		name = "\"Ignore\" menu entry color",
		description = "Text color of the right-click \"Add to spellcheck ignore list\" menu entry.",
		position = 20,
		section = appearanceSection
	)
	default Color ignoreMenuColor()
	{
		return new Color(255, 180, 50);
	}
}
