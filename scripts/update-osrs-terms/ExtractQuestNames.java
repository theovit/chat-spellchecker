import net.runelite.api.Quest;

/**
 * Standalone (no package, no build.gradle involvement) - prints every OSRS quest's display name,
 * one per line, using RuneLite's own {@code Quest} enum as the source of truth. Compiled and run
 * directly against a downloaded {@code runelite-api} jar by the update-osrs-terms workflow; never
 * part of the plugin's own source sets, so it's never bundled into the plugin jar.
 *
 * {@link Quest#getName()} is pure static data - no live client or game cache needed.
 */
public class ExtractQuestNames
{
	public static void main(String[] args)
	{
		for (Quest quest : Quest.values())
		{
			System.out.println(quest.getName());
		}
	}
}
