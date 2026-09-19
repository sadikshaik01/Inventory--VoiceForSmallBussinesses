package com.voicestock;
import com.voicestock.service.CommandParser;
import com.voicestock.dto.AssistantModels.Intent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.assertj.core.api.Assertions.*;

class NaturalCommandTests {
    final CommandParser parser=new CommandParser();
    @ParameterizedTest @CsvSource({
        "Add 10 packets of chips,ADD_STOCK,chips,10,PACKETS", "Add 10 packets chips,ADD_STOCK,chips,10,PACKETS", "Chips add 10 packets,ADD_STOCK,chips,10,PACKETS",
        "Chips 10 packets add,ADD_STOCK,chips,10,PACKETS", "10 packets chips add,ADD_STOCK,chips,10,PACKETS", "Received 10 packets of chips,ADD_STOCK,chips,10,PACKETS",
        "Restock chips with 10 packets,ADD_STOCK,chips,10,PACKETS", "Sold 2 packets of chips,REMOVE_STOCK,chips,2,PACKETS", "Take out 2 packets chips,REMOVE_STOCK,chips,2,PACKETS",
        "10 packets of chips,UNKNOWN,chips,10,PACKETS", "Add 2 packs of potato chips,ADD_STOCK,potato chips,2,PACKETS", "Add 2 ltrs of sunflower oil,ADD_STOCK,sunflower oil,2,LITRES",
        "Rice 5 bags add cheyandi,ADD_STOCK,rice,5,BAGS", "Rice stock 5 bags penchu,ADD_STOCK,rice,5,BAGS", "Rice 2 bags tagginchu,REMOVE_STOCK,rice,2,BAGS",
        "Rice nundi 2 bags teeseyi,REMOVE_STOCK,rice,2,BAGS", "Rice 2 bags tagginchandi,REMOVE_STOCK,rice,2,BAGS", "Rice 5 bags jodo,ADD_STOCK,rice,5,BAGS",
        "Rice 2 bags ghatao,REMOVE_STOCK,rice,2,BAGS", "Rice 2 bags nikalo,REMOVE_STOCK,rice,2,BAGS",
        "राइस 10 बैग जोड़ो,ADD_STOCK,rice,10,BAGS", "राइस में 5 बैग ऐड करो,ADD_STOCK,rice,5,BAGS", "राइस में से 2 बैग हटाओ,REMOVE_STOCK,rice,2,BAGS",
        "राइस २ बैग निकालो,REMOVE_STOCK,rice,2,BAGS"
    }) void natural(String text,Intent intent,String product,String qty,String unit) {
        var c=parser.parse(text);assertThat(c.intent()).isEqualTo(intent);assertThat(c.product()).isEqualTo(product);assertThat(c.quantity()).isEqualByComparingTo(qty);assertThat(c.unit()).isEqualTo(unit);
    }
    @ParameterizedTest @CsvSource({
        "What needs my attention?,ATTENTION,", "What should I reorder?,REORDER,", "What is running out soon?,RUNNING_OUT,", "Why should I reorder rice?,REORDER_REASON,rice",
        "How long will rice last?,STOCK_DURATION,rice", "What actions are pending?,PENDING_ACTIONS,", "What reorders did I approve?,APPROVED_REORDERS,",
        "Rice entha migilindi?,CHECK_STOCK,rice", "Rice kitna bacha hai?,CHECK_STOCK,rice", "राइस स्टॉक कितना है,CHECK_STOCK,rice"
    }) void questions(String text,Intent intent,String product) { var c=parser.parse(text);assertThat(c.intent()).isEqualTo(intent);assertThat(c.product()).isEqualTo(product); }
    @Test void partialFields() { assertThat(parser.parse("10 packets chips").missingFields()).containsExactly("intent");assertThat(parser.parse("Add chips").missingFields()).containsExactly("quantity","unit"); }
}
