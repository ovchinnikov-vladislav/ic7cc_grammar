package ic7cc.ovchinnikov.lab2.model;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Objects;

@Getter
@ToString
public class Terminal extends Symbol {

    private final String spell;

    public Terminal(String name, String spell) {
        super(name, Type.TERM);
        this.spell = spell;
    }

    public static final Terminal EPSILON = new Terminal("EPSILON", "eps");

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Terminal terminal = (Terminal) o;
        return Objects.equals(spell, terminal.spell) &&
                Objects.equals(getName(), terminal.getName()) &&
                getType() == terminal.getType();
    }

    @Override
    public int hashCode() {
        return Objects.hash(spell, getName(), getType());
    }
}
