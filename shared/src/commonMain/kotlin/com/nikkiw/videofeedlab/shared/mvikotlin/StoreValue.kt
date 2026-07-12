package com.nikkiw.videofeedlab.shared.mvikotlin

import com.arkivanov.decompose.Cancellation
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.Lifecycle
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.arkivanov.mvikotlin.core.rx.observer
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.rx.observer as mviObserver

fun <State : Any> Store<*, State, *>.asValue(): Value<State> =
    object : Value<State>() {
        override val value: State
            get() = this@asValue.state

        override fun subscribe(observer: (State) -> Unit): Cancellation {
            val disposable =
                this@asValue.states(
                    mviObserver(onNext = observer),
                )

            return Cancellation {
                disposable.dispose()
            }
        }
    }

fun <State : Any> Store<*, State, *>.asValue(lifecycle: Lifecycle): Value<State> {
    val value = MutableValue(state)
    val disposable =
        states(
            observer(
                onNext = { state -> value.value = state },
            ),
        )

    lifecycle.doOnDestroy {
        disposable.dispose()
        dispose()
    }

    return value
}

fun <State : Any, Model : Any> Store<*, State, *>.asValue(
    lifecycle: Lifecycle,
    mapper: (State) -> Model,
): Value<Model> {
    val value = MutableValue(mapper(state))
    val disposable =
        states(
            observer(
                onNext = { state -> value.value = mapper(state) },
            ),
        )

    lifecycle.doOnDestroy {
        disposable.dispose()
    }

    return value
}
