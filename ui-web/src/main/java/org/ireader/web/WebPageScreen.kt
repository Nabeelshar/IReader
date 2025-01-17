
package org.ireader.web

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.SwipeRefreshIndicator
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.google.accompanist.web.AccompanistWebChromeClient
import com.google.accompanist.web.AccompanistWebViewClient
import com.google.accompanist.web.WebView
import com.google.accompanist.web.rememberWebViewState
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.ireader.core_api.http.setDefaultSettings
import org.ireader.core_api.source.CatalogSource
import org.ireader.core_api.source.HttpSource
import org.ireader.core_ui.ui.SnackBarListener

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@ExperimentalCoroutinesApi
@Composable
fun WebPageScreen(
    modifier: Modifier = Modifier,
    viewModel: WebViewPageModel,
    onPopBackStack: () -> Unit,
    onModalBottomSheetShow: () -> Unit,
    onModalBottomSheetHide: () -> Unit,
    source: CatalogSource?,
    snackBarHostState: SnackbarHostState,
    scaffoldPadding: PaddingValues
) {
    val userAgent = remember {
        (source as? HttpSource )?.getCoverRequest("")?.second?.headers?.get(HttpHeaders.UserAgent)
    }
    val webViewState = rememberWebViewState(url = viewModel.url)


    LaunchedEffect(key1 = webViewState.hashCode()) {
        viewModel.webViewState = webViewState
    }


    LaunchedEffect(key1 = webViewState.content.getCurrentUrl()) {
        webViewState.content.getCurrentUrl()?.let { viewModel.webUrl = it }
    }
    val chromeClient = AccompanistWebChromeClient()
    val webclient = AccompanistWebViewClient()

    webViewState.content.getCurrentUrl()

    SnackBarListener(vm = viewModel, host = snackBarHostState)
    val refreshState = rememberSwipeRefreshState(isRefreshing = viewModel.isLoading)
    Box(modifier = Modifier.padding(scaffoldPadding)) {
        SwipeRefresh(
            state = refreshState,
            onRefresh = {
                viewModel.webView?.reload()
                viewModel.toggleLoading(true)
            },
            indicator = { state, trigger ->
                SwipeRefreshIndicator(
                    state = state,
                    refreshTriggerDistance = trigger,
                    scale = true,
                    backgroundColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.primaryContainer,
                    elevation = 8.dp
                )
            }
        ) {
            if (webViewState.isLoading) {
                LinearProgressIndicator(
                    Modifier
                        .fillMaxWidth(),
                )
            }
            WebView(
                state = webViewState,
                onCreated = {
                    userAgent?.let { ua -> it.setUserAgent(ua) }
                    it.setDefaultSettings()
                    viewModel.webView = it
                },
                chromeClient = chromeClient,
                client = webclient
            )
        }
    }
}
