/**
 * Copyright 2018-2019 rideOS, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.rideos.android.common.viewmodel.progress;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.Disposables;
import io.reactivex.subjects.BehaviorSubject;
import timber.log.Timber;

public class ProgressSubject {
    public enum ProgressState {
        IDLE,
        LOADING,
        SUCCEEDED,
        FAILED
    }

    private final BehaviorSubject<ProgressState> stateSubject;

    public ProgressSubject() {
        this(ProgressState.IDLE);
    }

    public ProgressSubject(final ProgressState progressState) {
        this.stateSubject = BehaviorSubject.createDefault(progressState);
    }

    public Observable<ProgressState> observeProgress() {
        return stateSubject.distinctUntilChanged();
    }

    public ProgressState getProgress() {
        return stateSubject.getValue();
    }

    public void idle() {
        stateSubject.onNext(ProgressState.IDLE);
    }

    public void started() {
        stateSubject.onNext(ProgressState.LOADING);
    }

    public void succeeded() {
        stateSubject.onNext(ProgressState.SUCCEEDED);
    }

    public void failed() {
        stateSubject.onNext(ProgressState.FAILED);
    }

    public Disposable followAsyncOperation(final Completable asyncOperation) {

        switch (getProgress()) {
            case IDLE:
            case FAILED:
                started();
                return asyncOperation.subscribe(
                    this::succeeded,
                    throwable -> {
                        Timber.e(throwable);
                        failed();
                    }
                );
        }
        return Disposables.empty();
    }
}
