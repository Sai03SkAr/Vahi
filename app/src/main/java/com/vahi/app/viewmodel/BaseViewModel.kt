package com.vahi.app.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Base ViewModel providing common functionality for all ViewModels in the app.
 * Uses StateFlow for reactive state management following MVVM pattern.
 *
 * @param State The type representing the UI state
 * @param initialState The initial state value
 */
abstract class BaseViewModel<State>(initialState: State) : ViewModel() {

    private val _uiState = MutableStateFlow(initialState)
    val uiState: StateFlow<State> = _uiState.asStateFlow()

    /**
     * Updates the current UI state.
     * @param reducer A function that takes the current state and returns the new state
     */
    protected fun updateState(reducer: (State) -> State) {
        _uiState.value = reducer(_uiState.value)
    }

    /**
     * Sets the UI state directly.
     * @param newState The new state to set
     */
    protected fun setState(newState: State) {
        _uiState.value = newState
    }
}

/**
 * Marker interface for UI state classes.
 * Implement this in your state data classes for type safety.
 */
interface UiState

/**
 * Marker interface for one-time UI events (e.g., navigation, snackbar).
 * Implement this in your event sealed classes.
 */
interface UiEvent
