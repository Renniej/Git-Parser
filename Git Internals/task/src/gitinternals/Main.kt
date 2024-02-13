package gitinternals

import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.zip.InflaterInputStream
import java.util.zip.ZipException

const val CHAR_DELIM = "\n"


class Blob(objFile : File) : GitObject(objFile) {

}

class ItemEntry(val hash : String, val permissions : String, val name : String)



class Tree(objFile : File) : GitObject(objFile) {

    private val database = mutableListOf<ItemEntry>()  //


    init {
        decrypt()
    }


    override fun toString() : String {
        var output = ""
        var debug = ""
        database.forEach{ itemEntry ->

            val permissions = itemEntry.permissions
            val hash = itemEntry.hash
            val fileName = itemEntry.name

            output+= "$permissions $hash $fileName\n"

            //if (hash == "4a8abe7b618ddf9c55adbea359ce891775794a61") debug+= super.data
        }

        return output.trim('\n')// + if (debug.isNotEmpty()) "\n\n${super.data}" else ""
    }

    fun getItems() = database.toList()


    // decrypts/decompresses the tree git file and then adds entries[permission metadata, filename, SHA-1 Hash] to the database
    private fun decrypt() {
        val inputStream = FileInputStream(objFile)
        val fileStream = InflaterInputStream(inputStream)
        var bytes = mutableListOf<Int>()

        while (fileStream.available() != 0) {
            bytes += fileStream.read()
        }

        bytes = removeHeader(bytes)  //removes header as that data is already parsed by our super class

        while (bytes.isNotEmpty()) {
            bytes = createEntry(bytes)
        }

    }




    //Converts the byte array into a string
    private fun convertBytesToString(bytes : List<Int>) : String = bytes.map { byte -> byte.toChar() }.joinToString("")

    //Converts the byte array into a string representing the hexadecimal
    private fun convertBytesToHexa(bytes : List<Int>) : String =   bytes.map {byte -> "%02x".format(byte)}.joinToString("")


    //takes an array of bytes and parses the first set of bytes representing an item (tree,file, subdirectory), add it to the database and then removes it from the byteArray
    private fun createEntry(bytes : MutableList<Int>) : MutableList<Int> {

        val nullIndex = findNullTerminated(bytes)

        val metadata = bytes.subList(0, nullIndex).toList() //get bytes representing permission metadata number, a whitespace, a filename
        val (permissions, fileName) = convertBytesToString(metadata).split(" ") //convert bytes to string and parse it from permission metanumber and filename

        val hashBytes = bytes.subList(nullIndex+1, nullIndex+21)  //get bytes representing the sha1hash
        val sha1Hash = convertBytesToHexa(hashBytes) //convert bytes to hexadecimal and represent it using a string

        database += ItemEntry(sha1Hash, permissions, fileName) //add sha1hash,permission and file name to database

        return bytes.subList( nullIndex+21, bytes.size) //remove parsed bytes from bytes array
    }


    //Returns the index of the byte representing the first null-terminated character in the byte array
    private fun findNullTerminated(bytes : MutableList<Int>) : Int {
        val index =  bytes.indexOfFirst{ num -> num == '\u0000'.code}
        if (index == -1) throw Exception("Null Terminated Byte does not exist >_<")
        return index
    }

    //Takes a byte array and returns the remaining byte data after the header has been removed. (I do this because the super class already handles parsing the header_
    private fun removeHeader(bytes : MutableList<Int>) : MutableList<Int> {
        val index = findNullTerminated(bytes)
        return bytes.subList(index+1,bytes.size)
    }



}

class Branch(private val file: File, val name : String) {

    val curCommitHash = if (file.exists())
        String(file.readBytes()).trim('\n').trim(' ')
    else
        throw Exception("File does not exist: ${file.name}")


}




//Class representing
class UserData(val name : String, val email : String,  timestamp : String, timeOffset : String) {

    val timePublished : Instant = Instant.ofEpochSecond(timestamp.toLong())
    val offset = ZoneOffset.of(timeOffset)
    val timeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(offset)



    fun toText( label : String = "") : String {

        val lbl: String = if (label.isEmpty()) "timestamp: " else "$label timestamp: "

        return "$name $email $lbl${timeFormat.format(timePublished)} $offset"
    }

    override fun toString() : String {
        return this.toText()
    }


}



open class Commit(objFile : File) : GitObject(objFile) {


    //Change to val at a later date. ( I need to understand why I can't initialize a val variable within a fo
    val tree : String
    val author : UserData
    val  committer : UserData
    val  message : String
    val parents : List<String>
    private var merged = false



    init {


        var _message : String = ""
        var _tree  : String? = null
        var _parents = mutableListOf<String>()
        var _author : UserData? = null
        var _committer : UserData? = null

        val lines =  data.split("\n") //get all the lines in data





        for (line in lines) { // map information found in data to variables based on key (first word in the line)

            val content = line.split(" ").map { it.replace(Regex("[<>]"),"") }  // grabs fields and trims invalid characters such as "<", ">" and spaces
            val key = content[0]
            val fields = content.subList(1, content.size)


            when(key) {
                "tree" ->  _tree = fields[0]
                "parent" -> _parents += fields[0]
                "author" -> _author = UserData(fields[0],fields[1], fields[2],fields[3] )
                "committer" -> _committer = UserData(fields[0],fields[1],fields[2],fields[3])
                else ->  { //this is the commit message
                    var str = content.joinToString(" ")
                    _message += if (_message.isEmpty()) str else "\n$str"

                }
            }
        }


        when {
            _tree == null ->  { println("DATA : $data"); throw Exception("no tree key found in commit") }
            _author == null -> throw Exception("No author data found in commit")
            _committer == null -> throw Exception("No committer info found in commit")
        }


        //_message += "\n "

        message = _message
        tree = _tree!!
        parents = _parents.toList()
        author = _author!!
        committer = _committer!!
    }



    fun hasParent() : Boolean {
        return parents.isNotEmpty()
    }

    override fun toString() : String{



        return "tree: $tree\n" + (if (parents.isNotEmpty()) "parents: ${parents.joinToString(" | ")}\n" else "") +

                "author: ${author.toText("original")}\n" +
                "committer: ${committer.toText("commit")}\n" +
                "commit message:\n$message"


    }

    fun setMerged(bool : Boolean) {
        merged = bool
    }

    fun isMerged() : Boolean {
        return merged
    }


}




open class GitObject(protected val objFile : File) {

    val data : String
    val type : String
    val length : Int



    init {
        val _data = decompress()
        val metadata = _data.substringBefore('\n').split(" ")

        data =  _data.substringAfter('\n') //removes meta data from data



        type =  metadata.first()
        length =   metadata.last().toInt()


    }



    /**
     * decompresses and reads a git obj file
     * returns a string representing the contents of the git file
     * Parameters :
     *      obj : File - file object representing the gitObj file
     **/
    private fun decompress() : String {

        if (!objFile.exists()) throw Exception("Git object does not exist")

        val inputStream : InputStream = FileInputStream(objFile) //create input stream
        val inflateStream = InflaterInputStream(inputStream) // decompress zip file
        val characters = mutableListOf<Char>()


        while(inflateStream.available() != 0) {
            val byte  = inflateStream.read()
            val char =  byte.toChar()

            characters += if (char == '\u0000') {
                //if the char is a C++ null terminated char then convert it to a '\n' newline char
                '\n'
            } else
                char
        }





        inflateStream.close()
        inputStream.close()
        return characters.joinToString(separator = "")
    }


    fun type() : String = type

    override fun toString() : String {
        return data
    }

}


private fun convertBytesToString(bytes : ByteArray) : String =  String(bytes)



class GitManager(gitFolderPath : String) {

    private val gitFolder : File  // .git
    private val objFolder : File // .git/objects

    private val headsFolder : File // .git./refs/heads
    private val headFile : File  //.git/HEAD

    private var currentHead : String  //current branches name

    private var branchHashes = mutableMapOf<String,String>()  //Branch Name, Current Commit Hash


    init {
        gitFolder = validateFolder(gitFolderPath)
        objFolder = validateFolder("$gitFolderPath/objects")
        headsFolder = validateFolder("$gitFolderPath/refs/heads")

        headFile = validateFile("$gitFolderPath/HEAD")
        currentHead = getCurBranchFromFile()

        recordBranches()
    }


    fun executeCommand(cmd : String, arguments : String? = null) : String {

        val args = arguments ?: ""

        return when(cmd) {
            "list-branches" -> listBranches()
            "cat-file" -> catFile(args)
            "log" -> logFile(args)
            "commit-tree" -> commitTree(args)
            else -> "Invalid command : $cmd"
        }

    }


    private fun getTreeStack(tree : Tree, name : String)  : String {

        val items = tree.getItems()

        var folderName = "$name/"
        var output = ""

        for (item in items) {

            val obj = getGitObject(item.hash)

            output += when(obj.type) {
                "tree" -> folderName + getTreeStack(obj as Tree, item.name)
                else -> folderName + item.name
            }

        }

        return output
    }

    private fun commitTree(commitHash : String) : String{
        var output = ""

        val commit = getGitObject(commitHash) as Commit
        val tree = getGitObject(commit.tree) as Tree

        val items = tree.getItems()

        items.forEach { item->

            val obj = getGitObject(item.hash)

            output += when(obj.type) {
                "tree" -> getTreeStack(obj as Tree, item.name)
                else -> "${item.name}\n"
            }

        }


        return output
    }

    private fun recordBranches() = getAllBranches().forEach { branch -> branchHashes +=  branch.name to branch.curCommitHash}




    private fun getGitObject(hash : String) : GitObject{



        return createGitObject(hash)
    }



    private fun findMergedBranch(hash1 : String, hash2 : String, curBranch : Branch) : List<Branch> =  getAllBranches().filter { branch ->   (branch.curCommitHash == hash1 || branch.curCommitHash == hash2) && branch != curBranch  }







    private fun getCommitStack(hash : String, branch : Branch) : List<Pair<String,Commit>> {

        val commit = getGitObject(hash) as Commit
        val hashCommit = hash to commit

        val parents = commit.parents

        return when (parents.size) {
            1 -> listOf(hashCommit) + getCommitStack(commit.parents.first(),branch)
            2 ->  {
                val mergedBranch =  findMergedBranch(parents.first(), parents.last(), branch)[0]
                val mergedCommit = getGitObject(mergedBranch.curCommitHash) as Commit

                val mergedHashCommit = mergedBranch.curCommitHash to  mergedCommit

                mergedCommit.setMerged(true)

                val nextHash = when(mergedBranch.curCommitHash){
                    parents.first() -> parents.last()
                    else -> parents.first()
                }

                listOf(hashCommit) +  listOf(mergedHashCommit) + getCommitStack(nextHash, branch)

            }
            else -> listOf(hashCommit)
        }



    }

    private fun logFile(branchName : String)  : String {

        val branch = getBranch(branchName) ?: return "Invalid branch name : $branchName"

        val stack = getCommitStack(branch.curCommitHash, branch)
        var output = ""

        for ((hash,commit) in stack) {

            output += "Commit: $hash" + (if (commit.isMerged()) " (merged)\n" else "\n") +
                    "${commit.committer.toText("commit")}\n" +
                    "${commit.message}\n"
        }

        return output
    }

    private fun getBranch(name : String) = getAllBranches().find{ bran -> bran.name == name}

    private fun catFile(hash : String) : String {
        val gitObj = getGitObject(hash)

        return "*${gitObj.type().uppercase()}*\n" +
                gitObj.toString()

    }

    fun listBranches() : String {
        var str = ""
        val branches = getAllBranches()


        for (branch in branches) {
            if (branch.name == currentHead)
                str += "* ${branch.name}\n"
            else
                str += "  ${branch.name}\n"
        }

        return str
    }


    fun getAllBranches() : List<Branch> {

        val branches = mutableListOf<Branch>()
        val folders = headsFolder.listFiles()

        folders!!.forEach {branchFile ->
            branches += Branch(branchFile,branchFile.name)
        }

        return branches

    }




    private fun getCurBranchFromFile() : String {
        val data = convertBytesToString(headFile.readBytes()) // file looks like : [ ref: refs/heads/main ]
        val hash = data.split("/")[2].trim('\n')

        return hash
    }

    // Checks if the path is a valid folder and if it is returns a file else throws an exception
    private fun validateFolder(path : String) : File{

        val file = File(path)

        return when {
            !file.exists() -> throw Exception("[${file.path}] does not exist")
            !file.isDirectory -> throw Exception("[${file.path}] is not a folder")
            else -> file
        }

    }

    private fun validateFile(path : String) : File{

        val file = File(path)

        return when {
            !file.exists() -> throw Exception("[${file.path}] does not exist")
            !file.isFile -> throw Exception("[${file.path}] is not a file")
            else -> file
        }

    }


    fun createGitObject(hash : String) : GitObject {

        val folderName = hash.substring(0,2)
        val fileName = hash.substring(startIndex = 2)

        val folder : File = objFolder.listFiles()?.find { it.isDirectory && it.name == folderName } ?: throw Exception("Folder [$folderName] does not exist for obj [$hash]")
        val objFile  = folder.listFiles()?.find{ it.name == fileName} ?:  throw Exception("git Object [${fileName}] does not exist at ${folder.path}")

        val gitObject : GitObject =GitObject(objFile)

        return when(gitObject.type()){
            "commit" -> Commit(objFile)
            "blob" -> Blob(objFile)
            "tree" -> Tree(objFile)
            else -> throw Exception("Invalid git object type")
        }

    }

}



/**
 * Gets input from user regrading the path to a .git folder
 * returns path in a String object
 */
fun inputFolderPath() : String{
    println("Enter .git directory location:")
    return readln()
}

/**
 * Gets input from user regrading the git object hash they would like to search for
 * returns String representing a git object hash
 */
fun inputGitHash() : String {
    println("Enter git object hash:")
    return readln()
}

fun inputBranchName() : String{
    println("Enter branch name:")
    return readln()

}
//0eee6a98471a350b2c2316313114185ecaf82f0e

fun inputArgument(arg : String) :String {
    println("Enter $arg:")
    return readln()
}

fun inputCommand() : List<String?> {

    println("Enter command:")
    val cmd = readln()

    var arg : String? = null

    when(cmd) {
        "cat-file" -> arg = inputGitHash()
        "log" -> arg = inputBranchName()
        "commit-tree" -> arg = inputArgument("commit-hash")
        else->{}
    }

    return listOf(cmd,arg)
}


fun main() {

    val manager = GitManager(inputFolderPath())
    val (cmd,args) = inputCommand()

    println(manager.executeCommand(cmd!!,args))  //output cmd's output

    


}