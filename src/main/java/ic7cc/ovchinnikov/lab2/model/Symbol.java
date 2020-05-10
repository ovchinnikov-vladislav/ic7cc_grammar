package ic7cc.ovchinnikov.lab2.model;

import com.fasterxml.jackson.annotation.*;
import lombok.Getter;
import lombok.ToString;

import java.util.Objects;

@Getter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Symbol {

    private final String name;
    private final String spell;
    private final Type type;

    @JsonCreator
    public Symbol(@JsonProperty("name") String name, @JsonProperty("spell") String spell, @JsonProperty("type") Type type) {
        this.name = name;
        this.spell = spell;
        this.type = type;
    }

    public enum Type {
        @JsonProperty("nonterm")
        NON_TERM,
        @JsonProperty("term")
        TERM,
        @JsonProperty("eps")
        EPS
    }

    public static final Symbol EPSILON = new Symbol("EPSILON", "eps", Type.EPS);

    public Terminal isTerminalGetting() {
        if (type == Type.TERM)
            return new Terminal(name, spell);
        return null;
    }

    public NonTerminal isNonTerminalGetting() {
        return new NonTerminal(name);
    }

    @JsonIgnore
    public boolean isTerminal() {
        return Type.TERM == type && !isEpsilon();
    }

    @JsonIgnore
    public boolean isNonTerminal() {
        return Type.NON_TERM == type;
    }

    @JsonIgnore
    public boolean isEpsilon() {
        return Type.EPS == type;
    }

    public static Symbol of(Terminal terminal) {
        return new Symbol(terminal.getName(), terminal.getSpell(), Type.TERM);
    }

    public static Symbol of(NonTerminal nonTerminal) {
        return new Symbol(nonTerminal.getName(), null, Type.NON_TERM);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Symbol symbol = (Symbol) o;
        return Objects.equals(name, symbol.name) &&
                type == symbol.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type);
    }
}
