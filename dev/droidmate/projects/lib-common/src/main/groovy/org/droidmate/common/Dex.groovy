// Copyright (c) 2012-2016 Saarland University
// All rights reserved.
//
// Author: Konrad Jamrozik, jamrozik@st.cs.uni-saarland.de
//
// This file is part of the "DroidMate" project.
//
// www.droidmate.org

package org.droidmate.common

import java.nio.file.Files
import java.nio.file.Path

class Dex
{

  @Delegate
  private final Path path

  Dex(Path path)
  {
    assert path != null
    assert Files.isRegularFile(path)
    assert path.fileName.toString().endsWith(".dex")
    this.path = path
  }
}