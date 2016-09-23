package generalapps.vocal;

/**
 * Created by edeetee on 16/09/2016.
 */
public interface ListListener<T> {
    void OnItemAdded(T item, int pos);
    void OnItemRemoved(T item, int pos);
    void OnItemChanged(T item, int pos);
    void OnItemMoved(T item, int fromPos, int toPos);
}
