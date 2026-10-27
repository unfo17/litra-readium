package io.github.unfo17.litra.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.isRestricted
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.shared.util.toUrl
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.DefaultPublicationParser
import java.io.File
import java.io.IOException

@Singleton
class ReadiumService @Inject constructor(@ApplicationContext context: Context) {
    private val http = DefaultHttpClient()
    private val retriever = AssetRetriever(context.contentResolver, http)
    private val opener = PublicationOpener(DefaultPublicationParser(context, http, retriever, pdfFactory = null))
    suspend fun open(file: File): Publication = withContext(Dispatchers.IO) {
        val asset = retriever.retrieve(file.toUrl(isDirectory = false)).getOrElse { throw IOException(it.message) }
        val publication = opener.open(asset, allowUserInteraction = false).getOrElse { asset.close(); throw IOException(it.message) }
        if (!publication.conformsTo(Publication.Profile.EPUB) || publication.isRestricted) {
            publication.close()
            throw IOException("仅支持未加密的 EPUB 2/3 文件")
        }
        publication
    }
}
