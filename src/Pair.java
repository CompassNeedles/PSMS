import java.io.Serializable;

public class Pair<F, S> implements Serializable{

	private static final long serialVersionUID = 1L;

    private F first;

    public F getFirst() { return first; }

    private S second;

    public S getSecond() { return second; }

    public Pair(F first, S second) {
        this.first = first;
        this.second = second;
    }

    @Override
    public String toString() {
        return "(" + first + "," + second + ")";
    }

    @Override
    public int hashCode() {
        return first.hashCode() * 13 + second.hashCode();
    }

     @Override
     public boolean equals(Object o) {
         if (this == o) return true;
         if (o instanceof Pair) {
        	 @SuppressWarnings("unchecked")
			 Pair<F, S> pair = (Pair<F, S>) o;
             if (first != null ? !first.equals(pair.first) : pair.first != null) return false;
             if (second != null ? !second.equals(pair.second) : pair.second != null) return false;
             return true;
         }
         return false;
     }
 }

