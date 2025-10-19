package utils;

import java.util.List;
import java.util.ArrayList;
import java.util.function.Consumer;

/**
 * A generic abstract class representing the Observable part of the Observer design pattern.
 * 
 * @param <T> The type of the observer (e.g., an interface or class with callback methods)
 */
public abstract class ObservableSubject<T> {
	
    // List to hold registered observers
    private final List<T> observers = new ArrayList<>();

    /**
     * Registers an observer to receive notifications.
     *
     * @param observer The observer instance to be added
     */
    public void registerObserver(T observer) {
        observers.add(observer);
    }

    /**
     * Notifies all registered observers using a provided callback.
     * This allows the observable to call specific methods on each observer.
     *
     * @param callback A Consumer functional interface that defines the action to be taken on each observer
     */
    protected void notifyObservers(Consumer<T> callback) {
        for (T observer : observers) {
            callback.accept(observer); // Calls the given method on each observer
        }
    }
}
