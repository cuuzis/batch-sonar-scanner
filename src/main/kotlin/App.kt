import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.SystemUtils
import java.io.*
import java.net.URL


fun main(args: Array<String>) {


    val fileURLs = readLinesFromFile("links.txt")
    for (fileURL in fileURLs) {
        val fileName = fileURL.substringBefore(".zip").split("/").last()
        val projectName = fileName.split("-").first()
        val projectVersion = fileName.split("-")[1]
        val projectKey = "QC:$projectName"
        val projectFullName = "QC - $projectName".replace("_", " ")

        val zipFile = File("zipfile.zip")
        val url = URL("http://java.labsoft.dcc.ufmg.br/qualitas.class/corpus/$fileName")
        FileUtils.copyURLToFile(url, zipFile)

        val oldFiles = File(".").listFiles()

        // unzip
        val pb = ProcessBuilder("jar", "xf", "zipfile.zip")
        val p = pb.start()
        val stdin = RunnableDemo(p.inputStream)
        stdin.start()
        val stderr = RunnableDemo(p.errorStream)
        stderr.start()
        /*val p = pb.start()
        val reader = BufferedReader(InputStreamReader(p.inputStream))
        val allText = reader.use(BufferedReader::readText)
        print(allText)
        val stderr = BufferedReader(InputStreamReader(p.errorStream))
        val errText = stderr.use(BufferedReader::readText)
        print(errText)*/

        val returnCode = p.waitFor()
        if (returnCode != 0)
            throw Throwable("unzip error, return code $returnCode")

        // get unzipped file name
        val newFiles = File(".").listFiles()
        for (document in newFiles) {
            if (!oldFiles.contains(document)) {
                // make sonar-scanner.properties
                val srcFolder = document
                File(srcFolder.canonicalPath + File.separator + "sonar-scanner.properties").printWriter().use { pw ->
                    pw.println("sonar.projectKey=$projectKey")
                    pw.println("sonar.projectName=$projectFullName")
                    pw.println("sonar.projectVersion=$projectVersion")
                    pw.println("sonar.sources=.")
                    pw.println("sonar.language=java")
                    //pw.println("sonar.binaries=.")
                    //pw.println("sonar.java.binaries=.")
                    pw.println("sonar.host.url=http://sonar.inf.unibz.it")
                    //pw.println("sonar.host.url=http://localhost:9000")

                }

                //perform scan
                val scannerCmd =
                        if (SystemUtils.IS_OS_WINDOWS)
                        "sonar-scanner.bat"
                    else
                        "sonar-scanner"
                val pbScan = ProcessBuilder(scannerCmd, "-Dproject.settings=sonar-scanner.properties")
                        .directory(File(srcFolder.canonicalPath + File.separator))
                val pScan = pbScan.start()

                val stdinScan = RunnableDemo(pScan.inputStream)
                stdinScan.start()
                val stderrScan = RunnableDemo(pScan.errorStream)
                stderrScan.start()
                /*val readerScan = BufferedReader(InputStreamReader(pScan.inputStream))
                val allTextScan = readerScan.use(BufferedReader::readText)
                print(allTextScan)
                val stderrScan = BufferedReader(InputStreamReader(pScan.errorStream))
                val errTextScan = stderrScan.use(BufferedReader::readText)
                print(errTextScan)*/

                val returnCodeScan = pScan.waitFor()
                if (returnCodeScan != 0)
                //    throw Throwable("scan error, return code $returnCodeScan")
                    println("scan error, return code $returnCodeScan")


                //delete folder
                document.deleteRecursively()
                break
            }
        }
        // delete downloaded zip
        zipFile.deleteRecursively()
    }
}

fun readLinesFromFile(file: String): List<String> {
    val result = mutableListOf<String>()
    BufferedReader(FileReader(file)).use { br ->
        do {
            val line = br.readLine()
            if (line != null)
                result.add(line)
        } while (line != null)
    }
    return result
}
