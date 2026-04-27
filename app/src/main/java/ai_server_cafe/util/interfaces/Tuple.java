package ai_server_cafe.util.interfaces;

public class Tuple<I, J, K> {
    private final I i;
    private final J j;
    private final K k;
    public Tuple(I i, J j, K k) {
        this.i = i;
        this.j = j;
        this.k = k;
    }

    public I getFirst() {
        return this.i;
    }

    public J getSecond() {
        return this.j;
    }

    public K getThird() {
        return this.k;
    }
}
