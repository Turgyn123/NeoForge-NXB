package net.narutoxboruto.command.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.narutoxboruto.util.ModUtil;

import java.util.concurrent.CompletableFuture;

public class AffiliationArgument implements ArgumentType<String> {
    private static final DynamicCommandExceptionType ERROR_AFF_INVALID = new DynamicCommandExceptionType(
            (f) -> Component.translatable("argument.affiliation.invalid", f));

    public static AffiliationArgument affiliation() {
        return new AffiliationArgument();
    }

    public static String getAffiliation(CommandContext<CommandSourceStack> pContext, String pName) throws CommandSyntaxException {
        String argument = pContext.getArgument(pName, String.class);

        if (!ModUtil.AFF_LIST.contains(argument)) {
            throw ERROR_AFF_INVALID.create(argument);
        }
        else {
            return argument;
        }
    }

    public String parse(StringReader pReader) {
        String result = pReader.readUnquotedString();

        return result;
    }

    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> pContext, SuggestionsBuilder pBuilder) {

        return SharedSuggestionProvider.suggest(ModUtil.AFF_LIST, pBuilder);
    }
}

