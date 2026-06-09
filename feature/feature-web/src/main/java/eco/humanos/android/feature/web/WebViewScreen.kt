package eco.humanos.android.feature.web

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.speech.RecognizerIntent
import android.widget.Toast
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.json.JSONObject

/**
 * Embedded HumanOS web module in a [WebView], authenticated via the session
 * bridge (ADR-0006). Full-screen with a slim top bar (back / debug / share /
 * reload). A built-in diagnostic — a DOM probe + captured console warnings/errors
 * — is always shareable (🐞), so a blank-content issue can be diagnosed without a
 * desktop debugger. `setWebContentsDebuggingEnabled` also enables chrome://inspect.
 */
@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewScreen(
    moduleKey: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WebViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(moduleKey) { viewModel.load(moduleKey) }

    var webView by remember { mutableStateOf<WebView?>(null) }
    var canGoBack by remember { mutableStateOf(false) }
    var progress by remember { mutableIntStateOf(0) }
    val consoleLog = remember { mutableStateListOf<String>() }
    var domProbe by remember { mutableStateOf("(sondeo pendiente)") }
    var showPanel by remember { mutableStateOf(false) }

    // Native dictation for the chat — webkitSpeechRecognition doesn't work in the
    // WebView, so use the system recognizer and inject the transcript into the
    // page's textarea via a React-compatible value setter + input event.
    val voiceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val text = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                .orEmpty()
            if (text.isNotBlank()) {
                val js = "(function(){var el=document.querySelector('textarea');if(!el)return;" +
                    "var d=Object.getOwnPropertyDescriptor(window.HTMLTextAreaElement.prototype,'value');" +
                    "d.set.call(el,(el.value?el.value+' ':'')+" + JSONObject.quote(text) + ");" +
                    "el.dispatchEvent(new Event('input',{bubbles:true}));el.focus();})();"
                webView?.evaluateJavascript(js, null)
            }
        }
    }

    // File picker for the chat's attach (📎) button — the WebView's <input
    // type=file> does nothing unless onShowFileChooser launches a picker.
    var filePathCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    val fileChooser = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri ->
        filePathCallback?.onReceiveValue(if (uri != null) arrayOf(uri) else null)
        filePathCallback = null
    }

    BackHandler(enabled = true) {
        val wv = webView
        if (canGoBack && wv != null) wv.goBack() else onBack()
    }

    fun debugReport(): String = buildString {
        append("humanOS WebView — ${state.title}\n")
        append((state.url ?: "").substringBefore("#token="))
        append("\n\n— DOM probe —\n$domProbe\n\n— Consola (${consoleLog.size}) —\n")
        append(consoleLog.takeLast(25).joinToString("\n"))
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(state.title.ifBlank { "HumanOS" }, maxLines = 1) },
                navigationIcon = {
                    // One tap returns to the app's native tabs (the bottom bar
                    // reappears). WebView history is still walkable via the device
                    // back button. Fixes "had to press back many times".
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver a la app")
                    }
                },
                actions = {
                    if (moduleKey == "chat") {
                        IconButton(onClick = {
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(
                                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
                                )
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-CL")
                                putExtra(RecognizerIntent.EXTRA_PROMPT, "Dictá tu mensaje…")
                            }
                            runCatching { voiceLauncher.launch(intent) }
                        }) {
                            Icon(Icons.Filled.Mic, contentDescription = "Dictar")
                        }
                    }
                    // Always available — even when the page renders blank with no errors.
                    IconButton(onClick = { showPanel = !showPanel }) {
                        Icon(Icons.Filled.BugReport, contentDescription = "Diagnóstico")
                    }
                    IconButton(onClick = {
                        val report = debugReport()
                        // Copy to clipboard (reliable) + a toast, then also offer the
                        // share sheet. Felipe can paste the diagnostic anywhere.
                        runCatching {
                            val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cb.setPrimaryClip(ClipData.newPlainText("humanOS diag", report))
                        }
                        Toast.makeText(context, "Diagnóstico copiado al portapapeles", Toast.LENGTH_SHORT).show()
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, report)
                        }
                        runCatching {
                            context.startActivity(Intent.createChooser(send, "Compartir diagnóstico"))
                        }
                    }) {
                        Icon(Icons.Filled.Share, contentDescription = "Compartir diagnóstico")
                    }
                    IconButton(onClick = { webView?.reload() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Recargar")
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                // Edge-to-edge (enableEdgeToEdge) means adjustResize no longer
                // shrinks content for the keyboard; consume the IME inset here so
                // the WebView resizes and the chat compose box stays above the
                // keyboard (fixes "el teclado tapa el cuadro").
                .imePadding()
                .fillMaxSize(),
        ) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }

                state.error != null -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            state.error ?: "Error",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                        Button(onClick = onBack) { Text("Volver") }
                    }
                }

                state.url != null -> {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            WebView.setWebContentsDebuggingEnabled(true)
                            WebView(ctx).apply {
                                webView = this
                                CookieManager.getInstance().setAcceptCookie(true)
                                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                                with(settings) {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    databaseEnabled = true
                                    javaScriptCanOpenWindowsAutomatically = true
                                    setSupportMultipleWindows(false)
                                    useWideViewPort = true
                                    // overview mode mis-computes viewport height
                                    // for app-shell layouts → body collapsed to 0
                                    // (blank content). Off = use the real viewport.
                                    loadWithOverviewMode = false
                                    mediaPlaybackRequiresUserGesture = false
                                    mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                                    cacheMode = WebSettings.LOAD_DEFAULT
                                    userAgentString = "$userAgentString humanOSApp"
                                }
                                webChromeClient = object : WebChromeClient() {
                                    override fun onConsoleMessage(message: ConsoleMessage): Boolean {
                                        val lvl = message.messageLevel()
                                        if (lvl == ConsoleMessage.MessageLevel.ERROR ||
                                            lvl == ConsoleMessage.MessageLevel.WARNING
                                        ) {
                                            consoleLog.add(
                                                "[$lvl] ${message.message()} (${message.sourceId()}:${message.lineNumber()})",
                                            )
                                        }
                                        return true
                                    }

                                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                        progress = newProgress
                                    }

                                    override fun onShowFileChooser(
                                        webView: WebView?,
                                        callback: ValueCallback<Array<Uri>>?,
                                        params: FileChooserParams?,
                                    ): Boolean {
                                        filePathCallback?.onReceiveValue(null)
                                        filePathCallback = callback
                                        return try {
                                            fileChooser.launch("*/*")
                                            true
                                        } catch (e: Exception) {
                                            filePathCallback = null
                                            false
                                        }
                                    }
                                }
                                webViewClient = object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(
                                        view: WebView?,
                                        request: WebResourceRequest?,
                                    ): Boolean {
                                        val host = request?.url?.host ?: return false
                                        val firstParty = host.endsWith("humanos.eco") ||
                                            host.endsWith("empresa.eco")
                                        if (firstParty) return false
                                        runCatching {
                                            view?.context?.startActivity(
                                                Intent(Intent.ACTION_VIEW, request.url)
                                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                                            )
                                        }
                                        return true
                                    }

                                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                        consoleLog.clear()
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        canGoBack = view?.canGoBack() == true
                                        // Force concrete pixel heights (not vh/dvh,
                                        // which collapse in this WebView) so the
                                        // content is visible. Re-apply post-hydration.
                                        view?.evaluateJavascript(HEIGHT_FIX_JS, null)
                                        view?.postDelayed({
                                            view.evaluateJavascript(HEIGHT_FIX_JS, null)
                                            view.evaluateJavascript(DOM_PROBE_JS) { result ->
                                                domProbe = result.trim().trim('"').replace("\\\"", "\"")
                                            }
                                        }, 1600)
                                    }

                                    override fun onReceivedError(
                                        view: WebView?,
                                        request: WebResourceRequest?,
                                        error: WebResourceError?,
                                    ) {
                                        if (request?.isForMainFrame == true) {
                                            consoleLog.add("[NET] ${error?.errorCode}: ${error?.description} (${request.url})")
                                        }
                                    }
                                }
                                loadUrl(state.url!!)
                            }
                        },
                        onRelease = { it.destroy() },
                    )

                    if (progress in 1..99) {
                        LinearProgressIndicator(
                            progress = { progress / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopCenter),
                        )
                    }

                    if (showPanel) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .heightIn(max = 280.dp)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(
                                    "Diagnóstico (toca 📤 para compartir)",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text("DOM: $domProbe", style = MaterialTheme.typography.bodySmall)
                                if (consoleLog.isNotEmpty()) {
                                    Text(
                                        "Consola (${consoleLog.size}):",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    consoleLog.takeLast(12).forEach {
                                        Text(it, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Reports whether the page content is present (and how tall) vs the visible
 * shell — distinguishes a layout collapse (mainHeight ~0) from empty data
 * (mainChildren 0) from a redirect (unexpected url).
 */
/**
 * Force concrete pixel heights so the page content is visible. The web shell
 * uses viewport-height units (vh/dvh) that collapse to ~0 in this WebView
 * (probe showed bodyH:0 with real content present). We set html/body to the
 * real innerHeight and un-collapse any 0-height/overflow-hidden ancestor of
 * <main> — without touching position (keeps the header/sidebar intact).
 */
private const val HEIGHT_FIX_JS = """
(function(){try{
  var h=(window.innerHeight||600)+'px';
  var d=document.documentElement, b=document.body;
  d.style.minHeight=h; b.style.minHeight=h; b.style.height='auto'; b.style.overflowY='auto';
  var el=document.querySelector('main');
  while(el && el!==b){
    var cs=getComputedStyle(el);
    if(cs.overflowY==='hidden') el.style.overflowY='visible';
    if(el.clientHeight===0 && el.scrollHeight>5){ el.style.height='auto'; el.style.minHeight=h; }
    el=el.parentElement;
  }
}catch(e){}})();
"""

private const val DOM_PROBE_JS = """
(function(){
  try {
    var m = document.querySelector('main');
    var t = (document.body && document.body.innerText || '').replace(/\s+/g,' ').trim();
    return JSON.stringify({
      url: location.pathname,
      title: document.title,
      mainExists: !!m,
      mainChildren: m ? m.childElementCount : -1,
      mainH: m ? m.scrollHeight : -1,
      bodyH: document.body ? document.body.scrollHeight : -1,
      winH: window.innerHeight,
      textLen: t.length,
      sample: t.slice(0,140)
    });
  } catch(e){ return 'probe-error: ' + e.message; }
})();
"""
