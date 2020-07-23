package main.java.match.algebra;

import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public abstract /*sealed*/ class Either<L, R> /*permits Left, Right*/ {

    public abstract boolean isLeft();

    public abstract boolean isRight();

    public abstract Optional<L> left();

    public abstract Optional<R> right();

    public final <S> Either<S, R> map(Function<? super L, ? extends S> f) {
        return mapLeft(f);
    }

    public abstract <S> Either<S, R> mapLeft(Function<? super L, ? extends S> f);

    public abstract <A> Either<L, A> mapRight(Function<? super R, ? extends A> f);

    public abstract <S, A> Either<S, A> mapBi(Function<? super L, ? extends S> fLeft, Function<? super R, ? extends A> fRight);

    public abstract <A> Either<L, A> flatMap(Function<? super R, ? extends Either<L, ? extends A>> rf);

    public abstract Either<R, L> swap();

    public abstract L leftOrThrow(Supplier<? extends Throwable> le) throws Throwable;

    public abstract R rightOrThrow(Supplier<? extends Throwable> re) throws Throwable;

    public abstract Either<L, R> ifLeft(Consumer<? super L> laction);

    public abstract Either<L, R> ifRight(Consumer<? super R> raction);

    public final void forEach(BiConsumer<Optional<? super L>, Optional<? super R>> action) {
        action.accept(left(), right());
    }

    public static <L, R> Either<L, R> left(L lval) {
        return new Left<>(lval);
    }

    public static <L, R> Either<L, R> right(R rval) {
        return new Right<>(rval);
    }

    public static final class Left<L, R> extends Either<L, R> {

        private final L lval;

        @Override
        public boolean isLeft() {
            return true;
        }

        @Override
        public boolean isRight() {
            return false;
        }

        @Override
        public Optional<L> left() {
            return Optional.of(lval);
        }

        @Override
        public Optional<R> right() {
            return Optional.empty();
        }

        @Override
        public <S> Either<S, R> mapLeft(Function<? super L, ? extends S> f) {
            return new Left<>(f.apply(lval));
        }

        @Override
        public <A> Either<L, A> mapRight(Function<? super R, ? extends A> f) {
            return new Left<>(lval);
        }

        @Override
        public <S, A> Either<S, A> mapBi(Function<? super L, ? extends S> fLeft, Function<? super R, ? extends A> fRight) {
            return new Left<>(fLeft.apply(lval));
        }

        @Override
        public <A> Either<L, A> flatMap(Function<? super R, ? extends Either<L, ? extends A>> rf) {
            return new Left<>(lval);
        }

        @Override
        public Either<R, L> swap() {
            return new Right<>(lval);
        }

        @Override
        public L leftOrThrow(Supplier<? extends Throwable> le) {
            return lval;
        }

        @Override
        public R rightOrThrow(Supplier<? extends Throwable> re) throws Throwable {
            throw re.get();
        }

        @Override
        public Either<L, R> ifLeft(Consumer<? super L> laction) {
            laction.accept(lval);
            return this;
        }

        @Override
        public Either<L, R> ifRight(Consumer<? super R> raction) {
            return this;
        }

        @Override
        public String toString() {
            return '(' + lval.toString() + "<)";
        }

        @Override
        public int hashCode() {
            return toString().hashCode() ^ 7 + lval.hashCode();
        }


        private Left(L lval) {
            this.lval = Objects.requireNonNull(lval);
        }
    }

    public static final class Right<L, R> extends Either<L, R> {

        private final R rval;

        @Override
        public boolean isLeft() {
            return false;
        }

        @Override
        public boolean isRight() {
            return true;
        }

        @Override
        public Optional<L> left() {
            return Optional.empty();
        }

        @Override
        public Optional<R> right() {
            return Optional.of(rval);
        }

        @Override
        public <S> Either<S, R> mapLeft(Function<? super L, ? extends S> f) {
            return new Right<>(rval);
        }

        @Override
        public <A> Either<L, A> mapRight(Function<? super R, ? extends A> f) {
            return new Right<>(f.apply(rval));
        }

        @Override
        public <S, A> Either<S, A> mapBi(Function<? super L, ? extends S> fLeft, Function<? super R, ? extends A> fRight) {
            return new Right<>(fRight.apply(rval));
        }

        @Override
        public <A> Either<L, A> flatMap(Function<? super R, ? extends Either<L, ? extends A>> rf) {
            return new Right<>(rf.apply(rval).right().orElse(null));
        }

        @Override
        public Either<R, L> swap() {
            return new Left<>(rval);
        }

        @Override
        public L leftOrThrow(Supplier<? extends Throwable> le) throws Throwable {
            throw le.get();
        }

        @Override
        public R rightOrThrow(Supplier<? extends Throwable> re) {
            return rval;
        }

        @Override
        public Either<L, R> ifLeft(Consumer<? super L> laction) {
            return this;
        }

        @Override
        public Either<L, R> ifRight(Consumer<? super R> raction) {
            raction.accept(rval);
            return this;
        }

        @Override
        public String toString() {
            return "(>" + rval.toString() + ')';
        }

        @Override
        public int hashCode() {
            return rval.hashCode() + 3 ^ toString().hashCode();
        }

        private Right(R rval) {
            this.rval = Objects.requireNonNull(rval);
        }
    }

    @Override
    public abstract String toString();

    @Override
    public abstract int hashCode();

    private Either() {}
}
