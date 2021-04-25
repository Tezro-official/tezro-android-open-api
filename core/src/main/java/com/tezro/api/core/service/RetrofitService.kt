package com.tezro.api.core.service

import com.tezro.api.core.client.requests.IRequest
import com.tezro.api.core.client.requests.IRequestDelegate
import com.tezro.api.core.client.requests.Request
import com.tezro.api.core.client.responses.Error
import com.tezro.api.core.service.core.Service
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

abstract class RetrofitService : Service() {

    protected open fun <T, R> Call<T>.toServiceRequest(converter: (T) -> R): IRequest<R> {
        val request = Request<R>()

        addCancelBlock(this::cancel)

        return request.setCancelBlock {
            cancelBlocks.remove(this::cancel)
            this.cancel()
        }.setEnqueueBlock {
            val callback = createCallback(request, converter)
            enqueue(callback)
        }
    }

    protected open fun <T, R> createCallback(requestDelegate: IRequestDelegate<R>, converter: (T) -> R): Callback<T> = object :
        Callback<T> {
        override fun onResponse(call: Call<T>, response: Response<T>) {
            val responseBody = response.body()

            if (responseBody == null) {
                val errorBody = response.errorBody()

                val error = if (errorBody != null) Error(errorBody.string(), response.code())
                else Error(DEFAULT_ERROR_MESSAGE, response.code())

                requestDelegate.notifyError(error)
                return
            }

            val result = converter.invoke(responseBody)
            requestDelegate.notifySuccess(result)
        }

        override fun onFailure(call: Call<T>, t: Throwable) {
            val error = Error(t.message ?: DEFAULT_ERROR_MESSAGE, DEFAULT_ERROR_CODE)
            requestDelegate.notifyError(error)
        }

    }

    companion object {
        const val DEFAULT_ERROR_MESSAGE = "UNKNOWN_ERROR"
        const val DEFAULT_ERROR_CODE = -1
    }

}