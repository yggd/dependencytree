package org.yggd.dependencytree.reader

import org.apache.http.HttpStatus
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.nio.file.Path
import java.nio.file.Paths

sealed interface PomReader {

    fun read(groupId: String, artifactId: String, version: String) : InputStream?

    fun replacedGroupId(groupId: String) = groupId.replace(".", File.separator)
    fun pomFileName(artifactId: String, version: String) = "${artifactId}-${version}.pom"
}

class CompositePomReader : PomReader {

    private val pomReaders = arrayOf(LocalRepositoryPomReader(), RemoteRepositoryPomReader())

    override fun read(groupId: String, artifactId: String, version: String): InputStream? {
        for (pomReader in pomReaders) {
            val i = pomReader.read(groupId, artifactId, version)
            if (i != null) {
                return i
            }
        }
        return null
    }
}

private class LocalRepositoryPomReader(
    private val localRepositoryPath: Path = Paths.get(System.getProperty("user.home"), ".m2", "repository")
) : PomReader {

    override fun read(groupId: String, artifactId: String, version: String): InputStream? =
        createReader(groupId, artifactId, version)

    private fun createReader(groupId: String, artifactId: String, version: String) : InputStream? {
        val replacedGroupId = replacedGroupId(groupId)
        val pomFileName = pomFileName(artifactId, version)
        val pomFile = localRepositoryPath
            .resolve(replacedGroupId)
            .resolve(artifactId)
            .resolve(version)
            .resolve(pomFileName)
            .toFile()
        if (!pomFile.exists() || !pomFile.isFile || !pomFile.canRead()) {
            return null
        }
        return FileInputStream(pomFile)
    }
}

private class RemoteRepositoryPomReader(
    private val remoteRepositoryUrl : String = "https://repo1.maven.org/maven2/"
) : PomReader {

    companion object {
        private val logger = LoggerFactory.getLogger(LocalRepositoryPomReader::class.java)
    }

    override fun read(groupId: String, artifactId: String, version: String): InputStream? {
        var url = remoteRepositoryUrl
        if (!url.endsWith("/")) {
            url = "${url}/"
        }

        val replacedGroupId = replacedGroupId(groupId)
        val pomFileName = pomFileName(artifactId, version)

        url = "${url}${replacedGroupId}/${artifactId}/${version}/${pomFileName}"

        HttpClients.createDefault().use { client ->
            val httpGet = HttpGet(url)
            client.execute(httpGet).use { response ->
                val statusCode = response.statusLine.statusCode
                if (HttpStatus.SC_OK != statusCode) {
                    logger.error("{} returned status code: {}", url, statusCode)
                    return null
                }
                val entity = EntityUtils.toByteArray(response.entity)
                return ByteArrayInputStream(entity)
            }
        }
    }

}