/*
 * Copyright 2016 Maxim Tuev.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.agna.ferro.mvprx;

import android.support.annotation.CallSuper;

import com.agna.ferro.mvp.presenter.MvpPresenter;
import com.agna.ferro.mvp.view.BaseView;
import com.agna.ferro.rx.OperatorFreeze;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Consumer;
import io.reactivex.subjects.BehaviorSubject;

/**
 * Presenter with freeze logic.
 * If subscribe to {@link Observable} via one of {@link #subscribe(Observable, Subscriber)} method,
 * all rx events (onNext, onError, onComplete) would be frozen when view destroyed and unfrozen
 * when view recreated (see {@link OperatorFreeze}).
 *
 * When screen finally destroyed, all subscriptions would be automatically unsubscribed.
 *
 * When configuration changed, presenter isn't destroyed and reused for new view
 *
 * If option freezeEventOnPause enabled (see {@link #setFreezeOnPauseEnabled(boolean)}, all events
 * would be also frozen when screen paused and unfrozen when screen resumed.
 * If option freezeEventOnPause disabled, screen may handle event when it invisible
 * (e.g. when Activity in back stack) and user can miss important information e.g. SnackBar
 */
public class MvpRxPresenter<V extends BaseView> extends MvpPresenter<V> {

    private final CompositeDisposable subscriptions = new CompositeDisposable();
    private final BehaviorSubject<Boolean> freezeSelector = BehaviorSubject.createDefault(false);
    private boolean freezeEventsOnPause = true;

    /**
     * This method is called, when view is ready
     * @param viewRecreated - show whether view created in first time or recreated after
     *                        changing configuration
     */
    @Override
    public void onLoad(boolean viewRecreated) {
        super.onLoad(viewRecreated);
    }

    @CallSuper
    @Override
    public void onLoadFinished() {
        super.onLoadFinished();
        freezeSelector.onNext(false);
    }

    @CallSuper
    @Override
    public void onResume() {
        super.onResume();
        freezeSelector.onNext(false);
    }

    @CallSuper
    @Override
    public void onPause() {
        super.onPause();
        if(freezeEventsOnPause) {
            freezeSelector.onNext(true);
        }
    }


    @CallSuper
    @Override
    protected void onViewDetached() {
        super.onViewDetached();
        freezeSelector.onNext(true);
    }

    @CallSuper
    @Override
    public void onDestroy() {
        super.onDestroy();
        subscriptions.dispose();
    }

    /**
     * If true, all rx event would be frozen when screen paused, and unfrozen when screen resumed,
     * otherwise event would be frozen when {@link #onViewDetached()} called.
     * Default enabled.
     * @param enabled
     */
    public void setFreezeOnPauseEnabled(boolean enabled) {
        this.freezeEventsOnPause = enabled;
    }

    /**
     * Apply {@link OperatorFreeze} and subscribe subscriber to the observable.
     * When screen finally destroyed, all subscriptions would be automatically unsubscribed.
     * For more information see description of this class.
     * @return subscription
     */
    private <T> Disposable subscribe(final Observable<T> observable,
                                       final OperatorFreeze<T> operator,
                                       final Subscriber<T> subscriber) {
        Disposable subscription = observable
                .lift(operator)
                .subscribe(subscriber);
        subscriptions.add(subscription);
        return subscription;
    }

    /**
     * @see #subscribe(Observable, OperatorFreeze, Subscriber)
     */
    private <T> Disposable subscribe(final Observable<T> observable,
                                       final OperatorFreeze<T> operator,
                                       final Consumer<T> onNext,
                                       final Consumer<Throwable> onError) {
        return subscribe(observable, operator,
                new Subscriber<T>() {
                    @Override
                    public void onComplete() {
                        // do nothing
                    }

                    @Override
                    public void onError(Throwable e) {
                        try {
                            onError.accept(e);
                        } catch (Exception e1) {
                            e1.printStackTrace();
                        }
                    }

                    @Override
                    public void onSubscribe(Subscription s) {

                    }

                    @Override
                    public void onNext(T t) {
                        try {
                            onNext.accept(t);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
    }

    /**
     * @see #subscribe(Observable, OperatorFreeze, Subscriber)
     * @param replaceFrozenEventPredicate - used for reduce num element in freeze buffer
     *                                    @see OperatorFreeze
     */
    protected <T> Disposable subscribe(final Observable<T> observable,
                                         final BiFunction<T, T, Boolean> replaceFrozenEventPredicate,
                                         final Subscriber<T> subscriber) {

        return subscribe(observable, createOperatorFreeze(replaceFrozenEventPredicate), subscriber);
    }

    /**
     * @see @link #subscribe(Observable, OperatorFreeze, Subscriber)
     * @param replaceFrozenEventPredicate - used for reduce num element in freeze buffer
     *                                    @see @link OperatorFreeze
     */
    protected <T> Disposable subscribe(final Observable<T> observable,
                                         final BiFunction<T, T, Boolean> replaceFrozenEventPredicate,
                                         final Consumer<T> onNext,
                                         final Consumer<Throwable> onError) {

        return subscribe(observable, createOperatorFreeze(replaceFrozenEventPredicate), onNext, onError);
    }

    /**
     * @see @link #subscribe(Observable, OperatorFreeze, Subscriber)
     */
    protected <T> Disposable subscribe(final Observable<T> observable,
                                         final Subscriber<T> subscriber) {

        return subscribe(observable, this.<T>createOperatorFreeze(), subscriber);
    }

    /**
     * @see @link #subscribe(Observable, OperatorFreeze, Subscriber)
     */
    protected <T> Disposable subscribe(final Observable<T> observable,
                                         final Consumer<T> onNext,
                                         final Consumer<Throwable> onError) {

        return subscribe(observable, this.<T>createOperatorFreeze(), onNext, onError);
    }

    /**
     * Subscribe subscriber to the observable without applying {@link OperatorFreeze}
     * When screen finally destroyed, all subscriptions would be automatically unsubscribed.
     * @return subscription
     */
    protected <T> Disposable subscribeWithoutFreezing(final Observable<T> observable,
                                                        final Subscriber<T> subscriber) {

        Disposable subscription = observable
                .subscribe(subscriber);
        subscriptions.add(subscription);
        return subscription;
    }

    /**
     * @see @link #subscribeWithoutFreezing(Observable, Subscriber)
     */
    protected <T> Disposable subscribeWithoutFreezing(final Observable<T> observable,
                                                        final Consumer<T> onNext,
                                                        final Consumer<Throwable> onError) {

        return subscribeWithoutFreezing(observable, new Subscriber<T>() {
            @Override
            public void onComplete() {
                // do nothing
            }

            @Override
            public void onError(Throwable e) {
                try {
                    onError.accept(e);
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }

            @Override
            public void onSubscribe(Subscription s) {

            }

            @Override
            public void onNext(T t) {
                try {
                    onNext.accept(t);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }


    protected <T> OperatorFreeze<T> createOperatorFreeze(BiFunction<T, T, Boolean> replaceFrozenEventPredicate) {
        return new OperatorFreeze<>(freezeSelector, replaceFrozenEventPredicate);
    }

    protected <T> OperatorFreeze<T> createOperatorFreeze() {
        return new OperatorFreeze<>(freezeSelector);
    }

    protected boolean isSubscriptionInactive(Disposable subscription) {
        return subscription == null || subscription.isDisposed();
    }
}
