package ic7cc.ovchinnikov.lab2.model;

import lombok.*;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

@ToString
public class Production {

    private final NonTerminal lhs;
    private final List<Symbol> rhs;

    public Production(NonTerminal lhs, List<Symbol> rhs) {
        this.lhs = lhs;
        this.rhs = rhs;
    }

    public NonTerminal getLhs() {
        return lhs;
    }

    public List<Symbol> getRhs() {
        return Collections.unmodifiableList(rhs);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Production that = (Production) o;
        return Objects.equals(lhs, that.lhs) &&
                Objects.equals(rhs, that.rhs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lhs, rhs);
    }
}
