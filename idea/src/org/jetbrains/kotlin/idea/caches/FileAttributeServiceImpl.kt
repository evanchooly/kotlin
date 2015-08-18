/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.caches

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.FileAttribute
import com.intellij.util.io.DataInputOutputUtil

public class FileAttributeServiceImpl : FileAttributeService {
    val attributes: MutableMap<String, FileAttribute> = hashMapOf()

    override fun register(id: String, version: Int) {
        attributes[id] = FileAttribute(id, version, true)
    }

    override fun writeAttribute(id: String, file: VirtualFile, value: Int?): SavedData {
        val attribute = attributes[id] ?: throw IllegalArgumentException("Attribute with $id wasn't registered")

        val data = SavedData(value, timeStamp = file.timeStamp)

        attribute.writeAttribute(file).use {
            DataInputOutputUtil.writeTIME(it, data.timeStamp)
            DataInputOutputUtil.writeINT(it, data.value ?: -1)
        }

        return data
    }

    override fun readAttribute(id: String, file: VirtualFile): SavedData? {
        val attribute = attributes[id] ?: throw IllegalArgumentException("Attribute with $id wasn't registered")

        val stream = attribute.readAttribute(file) ?: return null
        return stream.use {
            val timeStamp = DataInputOutputUtil.readTIME(it)
            val value = DataInputOutputUtil.readINT(it)

            if (file.timeStamp == timeStamp) {
                SavedData(value, timeStamp)
            }
            else {
                null
            }
        }
    }
}

