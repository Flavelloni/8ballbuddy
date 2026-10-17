package org.example.eighballpatterns.data

internal fun fetchResourceText(url: String, onSuccess: (String) -> Unit, onError: (String) -> Unit, revalidate: Boolean = false) {
    js("fetch(url, { cache: revalidate ? 'no-cache' : 'default' }).then(function(response) { if (!response.ok) { throw new Error(response.status + ' ' + response.statusText); } return response.text(); }).then(function(text) { onSuccess(text); }).catch(function(error) { onError(String(error)); });")
}
