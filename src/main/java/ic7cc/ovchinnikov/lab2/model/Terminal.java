package ic7cc.ovchinnikov.lab2.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.Objects;

@Getter
public class Terminal {

    private final String name;
    private final String spell;

    @JsonCreator
    public Terminal(@JsonProperty("name") String name, @JsonProperty("spell") String spell) {
        this.name = name;
        this.spell = spell;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Terminal terminal = (Terminal) o;
        return Objects.equals(spell, terminal.spell) &&
                Objects.equals(name, terminal.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(spell, name);
    }

    public Symbol toSymbol() {
        return new Symbol(name, spell, Symbol.Type.TERM);
    }

    @Override
    public String toString() {
        return "Terminal{" +
                "spell='" + spell + '\'' +
                "name='" + name + '\'' +
                '}';
    }
}
