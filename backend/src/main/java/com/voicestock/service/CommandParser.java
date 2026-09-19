package com.voicestock.service;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.*;
import org.springframework.stereotype.Component;
import com.voicestock.dto.AssistantModels.*;

@Component
public class CommandParser {
    private static final Map<String,String> UNITS=new HashMap<>();
    private static final String ADD="stock in|came in|add|increase|received|receive|restock|put|penchu|penchandi|badhao|jodo";
    private static final String REMOVE="take out|stock out|kam karo|remove|subtract|reduce|sold|decrease|tagginchu|tagginchandi|teesey|teeseyi|hatao|ghatao|nikalo";
    static {
        aliases("PACKETS","packet packets pack packs pkt pkts"); aliases("PIECES","piece pieces pc pcs"); aliases("KG","kg kgs kilo kilos kilogram kilograms");
        aliases("GRAMS","g gm gms gram grams"); aliases("LITRES","l litre litres liter liters ltr ltrs");
        aliases("MILLILITRES","ml millilitre millilitres milliliter milliliters");
        aliases("BAGS","bag bags"); aliases("CARTONS","carton cartons"); aliases("BOXES","box boxes"); aliases("DOZENS","dozen dozens"); aliases("QUINTALS","quintal quintals");
    }
    private static void aliases(String unit,String words) { for(String word:words.split(" ")) UNITS.put(word,unit); }
    public static String unit(String input) { return input==null ? null : UNITS.getOrDefault(input.toLowerCase(Locale.ROOT),input.toUpperCase(Locale.ROOT)); }
    public static String normalized(String text) {
        String s=Normalizer.normalize(text,Normalizer.Form.NFKC).toLowerCase(Locale.ROOT).replace('’','\'');
        // Limited phonetic spelling normalization, shared with catalog matching; never creates products.
        String[][] words={{"राइस","rice"},{"बैग्स","bags"},{"बैग","bags"},{"पैकेट","packets"},{"चिप्स","chips"},{"जोड़ो","jodo"},{"जोडो","jodo"},{"बढ़ाओ","badhao"},{"ऐड","add"},{"करो","karo"},{"हटाओ","hatao"},{"निकालो","nikalo"},{"घटाओ","ghatao"},{"कम","kam"},{"कितना","kitna"},{"स्टॉक","stock"},{"है","hai"},{"मेरे","mere"},{"पास","paas"},{"में","mein"},{"से","se"},{"बचा","bacha"}};
        for(String[] word:words) s=s.replace(word[0],word[1]);
        StringBuilder digits=new StringBuilder(); for(char c:s.toCharArray()) digits.append(Character.isDigit(c)?(char)('0'+Character.digit(c,10)):c);
        return digits.toString().replaceAll("[?!,]"," ").replaceAll("\\s+"," ").trim();
    }
    private boolean has(String s,String expression) { return Pattern.compile("(?<![\\p{L}\\p{M}])(?:"+expression+")(?![\\p{L}\\p{M}])").matcher(s).find(); }
    public Command parse(String raw) {
        String s=normalized(raw);
        String language=raw.matches("(?s).*[\\u0900-\\u097F].*") ? (raw.matches("(?s).*[a-zA-Z].*")?"mixed":"hi") : has(s,"cheyyi|chey|cheyandi|penchu|penchandi|tagginchu|tagginchandi|teesey|teeseyi|entha|undi|migilindi|nundi|mein|karo|hatao|badhao|jodo|ghatao|nikalo|kam|kitna|mere|paas|hai") ? "mixed" : "en";
        Intent intent=Intent.UNKNOWN;
        boolean add=has(s,ADD), remove=has(s,REMOVE);
        boolean unsafe=has(s,"not|don't|dont|never|cancel|nahi|मत|नहीं") || (add && remove);
        if(!unsafe) {
            if(has(s,"actions are pending|pending actions")) intent=Intent.PENDING_ACTIONS;
            else if(has(s,"reorders did i approve|approved reorders")) intent=Intent.APPROVED_REORDERS;
            else if(has(s,"needs my attention|need attention")) intent=Intent.ATTENTION;
            else if(has(s,"running out soon")) intent=Intent.RUNNING_OUT;
            else if(has(s,"how long")) intent=Intent.STOCK_DURATION;
            else if(has(s,"why.*reorder")) intent=Intent.REORDER_REASON;
            else if(add) intent=Intent.ADD_STOCK;
            else if(remove) intent=Intent.REMOVE_STOCK;
            else if(has(s,"out of stock")) intent=Intent.OUT_OF_STOCK;
            else if(has(s,"reorder|ordered")) intent=Intent.REORDER;
            else if(has(s,"low")) intent=Intent.LOW_STOCK;
            else if(has(s,"how much|do i have|available|entha|kitna")) intent=Intent.CHECK_STOCK;
        }
        BigDecimal price=null,quantity=null; String unit=null;
        Matcher pm=Pattern.compile("\\bat\\s+([0-9]+(?:\\.[0-9]+)?)\\s*(?:rupees|rs)?\\b").matcher(s);
        if(pm.find()) { price=new BigDecimal(pm.group(1)); s=pm.replaceFirst(" "); }
        Matcher qm=Pattern.compile("(?<![\\w.])(-?\\d+(?:\\.\\d+)?)(?![\\w.])").matcher(s);
        if(qm.find()) { quantity=new BigDecimal(qm.group(1)); s=s.substring(0,qm.start())+" "+s.substring(qm.end()); }
        for(String token:s.split("\\s+")) if(UNITS.containsKey(token)) { unit=unit(token); s=s.replaceFirst("\\b"+Pattern.quote(token)+"\\b"," "); break; }
        s=s.replaceAll("\\b(?:"+ADD+"|"+REMOVE+"|how much|do i have|is available|mere paas|how long will|why should i reorder)\\b"," ");
        s=s.replaceAll("\\b(?:cheyyi|chey|cheyandi|karo|mein|se|stock|of|please|with|the|to|nundi|entha|undi|migilindi|kitna|hai|bacha|available|last)\\b"," ").replaceAll("\\s+"," ").trim();
        boolean productIntent=Set.of(Intent.ADD_STOCK,Intent.REMOVE_STOCK,Intent.CHECK_STOCK,Intent.STOCK_DURATION,Intent.REORDER_REASON,Intent.UNKNOWN).contains(intent);
        String product=productIntent && !s.isBlank() ? s : null;
        // A negation or conflicting actions needs a fresh request, not an inferred mutation.
        if(unsafe) { product=null; quantity=null; unit=null; }
        List<String> missing=new ArrayList<>();
        if(intent==Intent.UNKNOWN) missing.add("intent");
        if(productIntent && product==null) missing.add("product");
        if(Set.of(Intent.ADD_STOCK,Intent.REMOVE_STOCK,Intent.UNKNOWN).contains(intent)) { if(quantity==null) missing.add("quantity"); if(unit==null) missing.add("unit"); }
        return new Command(intent,product,quantity,unit,price,language,intent==Intent.UNKNOWN?new BigDecimal("0.5"):BigDecimal.ONE,missing,null);
    }
}
