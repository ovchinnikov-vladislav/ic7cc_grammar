package ic7cc.ovchinnikov.lab2.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.ToString;

import java.util.Objects;

@Getter
@ToString
public abstract class Symbol {

    private final String name;
    private final Type type;

    public Symbol(String name, Type type) {
        this.name = name;
        this.type = type;
    }

    public enum Type {
        @JsonProperty("nonterm")
        NON_TERM,
        @JsonProperty("term")
        TERM,
        @JsonProperty("term")
        EPS
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
