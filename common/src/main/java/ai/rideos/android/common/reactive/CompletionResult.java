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
package ai.rideos.android.common.reactive;

/**
 * This class represents successful or failed completion of a completable operation. In case of a FAILURE more
 * information can be provided via param {@code T} along with an exception instance.
 *
 * @param <T> An optional companion info object that helps in dealing with the failure.
 */
public class CompletionResult<T> {

    private final T failureInfoObject;
    private final ResultType resultType;
    private final Throwable throwable;

    private CompletionResult(
            final ResultType resultType, final Throwable throwable, final T failureInfoObject) {
        this.resultType = resultType;
        this.throwable = throwable;
        this.failureInfoObject = failureInfoObject;
    }

    public static <T> CompletionResult<T> success() {
        return new CompletionResult<>(ResultType.SUCCESS, null, null);
    }

    public static <T> CompletionResult<T> failure(final Throwable throwable) {
        return new CompletionResult<>(ResultType.FAILURE, throwable, null);
    }

    public static <T> CompletionResult<T> failure(final Throwable throwable, final T failureInfo) {
        return new CompletionResult<>(ResultType.FAILURE, throwable, failureInfo);
    }

    public boolean isSuccess() {
        return resultType == ResultType.SUCCESS;
    }

    public boolean isFailure() {
        return resultType == ResultType.FAILURE;
    }

    public T getFailureInfo() {
        return failureInfoObject;
    }

    public Throwable getError() {
        return throwable;
    }

    private enum ResultType {
        SUCCESS,
        FAILURE
    }
}
