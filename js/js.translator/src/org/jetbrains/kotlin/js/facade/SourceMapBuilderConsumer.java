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

package org.jetbrains.kotlin.js.facade;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.PairConsumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.js.backend.ast.JsLocation;
import org.jetbrains.kotlin.js.sourceMap.SourceMapBuilder;

import java.io.File;
import java.util.*;

public class SourceMapBuilderConsumer implements PairConsumer<SourceMapBuilder, Object> {
    @NotNull
    private final Set<File> sourceRoots;

    public SourceMapBuilderConsumer(@NotNull List<File> sourceRoots) {
        this.sourceRoots = new HashSet<>();
        for (File sourceRoot : sourceRoots) {
            this.sourceRoots.add(sourceRoot.getAbsoluteFile());
        }
    }

    @Override
    public void consume(SourceMapBuilder builder, Object sourceInfo) {
        if (sourceInfo instanceof PsiElement) {
            PsiElement element = (PsiElement) sourceInfo;
            PsiFile psiFile = element.getContainingFile();
            int offset = element.getNode().getStartOffset();
            Document document = psiFile.getViewProvider().getDocument();
            assert document != null;
            int line = document.getLineNumber(offset);
            int column = offset - document.getLineStartOffset(line);

            File file = new File(psiFile.getViewProvider().getVirtualFile().getPath());
            builder.addMapping(getPathRelativeToSourceRoots(file), line, column);
        }
        else if (sourceInfo instanceof JsLocation) {
            JsLocation location = (JsLocation) sourceInfo;
            builder.addMapping(location.getFile(), location.getStartLine(), location.getStartChar());
        }
    }

    @NotNull
    private String getPathRelativeToSourceRoots(@NotNull File file) {
        List<String> parts = new ArrayList<>();
        File currentFile = file.getAbsoluteFile();

        while (currentFile != null) {
            if (sourceRoots.contains(currentFile)) {
                if (parts.isEmpty()) {
                    break;
                }
                Collections.reverse(parts);
                return StringUtil.join(parts, File.separator);
            }
            parts.add(currentFile.getName());
            currentFile = currentFile.getParentFile();
        }
        return file.getName();
    }
}
