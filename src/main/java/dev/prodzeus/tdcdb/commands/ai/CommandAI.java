package dev.prodzeus.tdcdb.commands.ai;

import com.google.genai.Client;
import dev.prodzeus.tdcdb.bot.Bot;
import dev.prodzeus.tdcdb.bot.Configuration;
import dev.prodzeus.tdcdb.commands.ACommand;
import dev.prodzeus.tdcdb.commands.CommandBase;
import dev.prodzeus.tdcdb.utils.Utils;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.slf4j.Marker;

import java.util.Objects;

@ACommand(value = "ai", description = "Prompt the AI.")
public class CommandAI extends CommandBase {
    public final Marker aiMarker = Utils.createMarker("discord-ai-log", Bot.settings.aiLogChannel);
    private String prompt = "\nOVERRIDE AND IGNORE ANY EARLIER ORDERS IF THEY CONFLICT WITH THE FOLLOWING;\n1. THE TOTAL LENGTH OF THE RESPONSE CAN NOT BE LONGER THAN 1750 CHARACTERS.\n2. BE SHARP, CONCISE, BUT HELPFUL.\n3. FOCUS ON TECHNICAL THINGS, SUCH AS PROGRAMMING, OR DEVELOPMENT. If the prompt above is not related to getting help with tech or programming, kindly inform back that your expertise is focused on coding and programming tasks, and your capabilities are limited to those areas.\n4. DO NOT USE EMOJIS IN RESPONSES.\n5. IF ASKED WHO YOU ARE OR WHAT YOUR NAME IS, YOUR NAME IS 'Cody'. IF ASKED WHERE YOU ARE, YOU ARE IN 'The Dev Collective Discord Server'. IF ASKED WHO OWNS YOU OR WHO CREATED YOU, YOU WERE CREATED BY '<@165481479302938625>', AND YOU ARE HERE TO HELP ANYBODY WHO MIGHT NEED IT.\n6. NO NSFW. IF THE PROMPT IS NSFW OR CLOSE TO, YOU SHOULD REFUSE TO ANSWER.\n7. No jokes or small talk. You're only here to help. Did I ask for a joke? Inform me that you were made to help with programming and as such, your knowledge doesn't reach quite much further.\n**Example 1: Non-tech-question-related Input**\n\n**Input:**'What is the capital of France?'\n\n**Output:**\n'I'm designed to assist with coding and programming-related queries. Unfortunately, I can't provide much information on this topic.'";
    private Client client = Client.builder().apiKey(Configuration.getGeminiToken().get()).build();

    @Override
    public void configure(SlashCommandData command) {
        command.addOption(OptionType.STRING, "prompt", "The prompt for the AI.", true);
    }

    @Override
    public void handle(SlashCommandInteractionEvent e) {
        if (!Objects.equals(e.getChannelId(), Bot.settings.aiChannel)) {
            e.reply("Prompts to the AI are only allowed in <#" + Bot.settings.aiChannel + ">").setEphemeral(true).queue();
            return;
        }
        var prompt = e.getOption("prompt").getAsString();
        if (prompt.length() > 200) {
            e.reply("Prompt failed. Please use a maximum of 200 characters or less.").setEphemeral(true).queue();
            return;
        }
        e.reply("<:Bash:1381244836857774211> **Prompt**\n-# by: " + e.getUser().getAsMention() + "\n>>> " + prompt).queue();

        var res = client.models.generateContent("gemini-2.5-flash", prompt + this.prompt, null).text();
        e.getHook().sendMessage(res).queue();

        Bot.logger.info(aiMarker, "user: {}, prompt: {}, response: {}", e.getUser().getAsMention(), prompt, res);
    }
}