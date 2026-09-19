package com.voicestock;

import com.voicestock.service.CommandParser;
import com.voicestock.dto.AssistantModels.Intent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.assertj.core.api.Assertions.*;

class CommandParserTests {
    final CommandParser parser=new CommandParser();
    @ParameterizedTest @CsvSource({
        "Add 20 bags of rice,ADD_STOCK,rice,20,BAGS,en",
        "Add 20 bags rice,ADD_STOCK,rice,20,BAGS,en",
        "Remove 5 bags of rice,REMOVE_STOCK,rice,5,BAGS,en",
        "Take out 5 bags of rice,REMOVE_STOCK,rice,5,BAGS,en",
        "Add 10 kg sugar,ADD_STOCK,sugar,10,KG,en",
        "Add 10 kg sugar at 45 rupees,ADD_STOCK,sugar,10,KG,en",
        "Rice 5 bags add cheyyi,ADD_STOCK,rice,5,BAGS,mixed",
        "Rice 2 bags remove cheyyi,REMOVE_STOCK,rice,2,BAGS,mixed",
        "Rice 5 bags penchu,ADD_STOCK,rice,5,BAGS,mixed",
        "Rice stock 2 bags tagginchu,REMOVE_STOCK,rice,2,BAGS,mixed",
        "Rice mein 5 bags add karo,ADD_STOCK,rice,5,BAGS,mixed",
        "Rice mein se 2 bags hatao,REMOVE_STOCK,rice,2,BAGS,mixed",
        "Rice 5 bags badhao,ADD_STOCK,rice,5,BAGS,mixed",
        "Rice stock 2 bags kam karo,REMOVE_STOCK,rice,2,BAGS,mixed"
    }) void stockCommands(String text,Intent intent,String product,String quantity,String unit,String language) {
        var c=parser.parse(text); assertThat(c.intent()).isEqualTo(intent);assertThat(c.product()).isEqualTo(product);
        assertThat(c.quantity()).isEqualByComparingTo(quantity);assertThat(c.unit()).isEqualTo(unit);assertThat(c.language()).isEqualTo(language);
    }
    @ParameterizedTest @CsvSource({
        "How much rice do I have?,CHECK_STOCK,rice", "How much sugar is available?,CHECK_STOCK,sugar", "Do I have rice?,CHECK_STOCK,rice",
        "Rice entha undi?,CHECK_STOCK,rice", "Rice stock entha?,CHECK_STOCK,rice", "Mere paas kitna rice hai?,CHECK_STOCK,rice", "Rice kitna hai?,CHECK_STOCK,rice",
        "Which products are running low?,LOW_STOCK,", "What's low in stock?,LOW_STOCK,", "Which products are out of stock?,OUT_OF_STOCK,",
        "What needs to be ordered?,REORDER,", "What should I reorder?,REORDER,"
    }) void questions(String text,Intent intent,String product) { var c=parser.parse(text);assertThat(c.intent()).isEqualTo(intent);assertThat(c.product()).isEqualTo(product); }
    @Test void missingAndUnsafeInputsAreNotInvented() {
        assertThat(parser.parse("Add rice").quantity()).isNull();assertThat(parser.parse("Add rice").unit()).isNull();
        assertThat(parser.parse("Tell me a joke").intent()).isEqualTo(Intent.UNKNOWN);
        assertThat(parser.parse("Don't add 5 bags rice").intent()).isEqualTo(Intent.UNKNOWN);
        assertThat(parser.parse("Add and remove 5 bags rice").intent()).isEqualTo(Intent.UNKNOWN);
        assertThat(parser.parse("Add -5 bags rice").quantity()).isNegative();
        assertThat(parser.parse("Add 10 kg sugar at 45 rupees").price()).isEqualByComparingTo("45");
    }
}
