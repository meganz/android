package mega.privacy.android.app.presentation.extensions.paging

import androidx.paging.compose.LazyPagingItems

fun LazyPagingItems<*>.printLoadStates(): String {
    return """
        Refresh: 
            End of Pagination reached: ${loadState.refresh.endOfPaginationReached}
            ${loadState.refresh}
        Prepend: 
            End of Pagination reached: ${loadState.prepend.endOfPaginationReached}
            ${loadState.prepend}
        Append:
            End of Pagination reached: ${loadState.append.endOfPaginationReached}
            ${loadState.append}
    """.trimIndent()
}