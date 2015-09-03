// Copyright 2015 Google Inc. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.devtools.build.lib.bazel.rules.android.ndkcrosstools;

import com.google.common.collect.ImmutableList;
import com.google.devtools.build.lib.rules.cpp.CppConfiguration;
import com.google.devtools.build.lib.view.config.crosstool.CrosstoolConfig.CToolchain;
import com.google.devtools.build.lib.view.config.crosstool.CrosstoolConfig.CompilationMode;
import com.google.devtools.build.lib.view.config.crosstool.CrosstoolConfig.CompilationModeFlags;
import com.google.devtools.build.lib.view.config.crosstool.CrosstoolConfig.ToolPath;

/**
 * Crosstool definitions for ARM. These values are based on the setup.mk files in the Android NDK
 * toolchain directories.
 */
class ArmCrosstools {

  private final NdkPaths ndkPaths;

  ArmCrosstools(NdkPaths ndkPaths) {
    this.ndkPaths = ndkPaths;
  }

  ImmutableList<CToolchain.Builder> createCrosstools() {

    ImmutableList.Builder<CToolchain.Builder> builder = ImmutableList.builder();

    builder.add(createAarch64Toolchain());

    // The flags for aarch64 clang 3.5 and 3.6 are the same, they differ only in the LLVM version
    // given in their tool paths.
    builder.add(createAarch64ClangToolchain("3.5"));
    builder.add(createAarch64ClangToolchain("3.6"));

    // The Android NDK Make files create several sets of flags base on
    // arm vs armeabi-v7a vs armeabi-v7a-hard, and arm vs thumb mode, each for gcc 4.8 and 4.9,
    // resulting in:
    //    arm-linux-androideabi-4.8
    //    arm-linux-androideabi-4.8-v7a
    //    arm-linux-androideabi-4.8-v7a-hard
    //    arm-linux-androideabi-4.8-thumb
    //    arm-linux-androideabi-4.8-v7a-thumb
    //    arm-linux-androideabi-4.8-v7a-hard-thumb
    //    arm-linux-androideabi-4.9
    //    arm-linux-androideabi-4.9-v7a
    //    arm-linux-androideabi-4.9-v7a-hard
    //    arm-linux-androideabi-4.9-thumb
    //    arm-linux-androideabi-4.9-v7a-thumb
    //    arm-linux-androideabi-4.9-v7a-hard-thumb
    //
    // and similar for the Clang toolchains.

    // gcc-4.8 for arm doesn't have the gcov-tool.
    CppConfiguration.Tool[] excludedTools = { CppConfiguration.Tool.GCOVTOOL };
    createArmeabiToolchain(builder, "4.8", "-fstack-protector", false, excludedTools);
    createArmeabiToolchain(builder, "4.8", "-fstack-protector", true, excludedTools);
    createArmeabiToolchain(builder, "4.9", "-fstack-protector-strong", false);
    createArmeabiToolchain(builder, "4.9", "-fstack-protector-strong", true);

    createArmeabiClangToolchain(builder, "3.5", false);
    createArmeabiClangToolchain(builder, "3.5", true);
    createArmeabiClangToolchain(builder, "3.6", false);
    createArmeabiClangToolchain(builder, "3.6", true);

    return builder.build();
  }

  private CToolchain.Builder createAarch64Toolchain() {

    String toolchainName = "aarch64-linux-android-4.9";
    String targetPlatform = "aarch64-linux-android";

    return CToolchain.newBuilder()
        .setToolchainIdentifier("aarch64-linux-android-4.9")
        .setTargetSystemName("aarch64-linux-android")
        .setTargetCpu("arm64-v8a")
        .setCompiler("gcc-4.9")

        .addAllToolPath(ndkPaths.createToolpaths(toolchainName, targetPlatform))

        .addAllCxxBuiltinIncludeDirectory(
            ndkPaths.createToolchainIncludePaths(toolchainName, targetPlatform, "4.9"))

        .setBuiltinSysroot(ndkPaths.createBuiltinSysroot("arm64"))

        .setSupportsEmbeddedRuntimes(true)
        .setStaticRuntimesFilegroup("static-runtime-libs-" + toolchainName)
        .setDynamicRuntimesFilegroup("dynamic-runtime-libs-" + toolchainName)

        // Compiler flags
        .addCompilerFlag("-fpic")
        .addCompilerFlag("-ffunction-sections")
        .addCompilerFlag("-funwind-tables")
        .addCompilerFlag("-fstack-protector-strong")
        .addCompilerFlag("-no-canonical-prefixes")

        // Linker flags
        .addLinkerFlag("-no-canonical-prefixes")

        // Additional release flags
        .addCompilationModeFlags(
            CompilationModeFlags.newBuilder()
                .setMode(CompilationMode.OPT)
                .addCompilerFlag("-O2")
                .addCompilerFlag("-g")
                .addCompilerFlag("-DNDEBUG")
                .addCompilerFlag("-fomit-frame-pointer")
                .addCompilerFlag("-fstrict-aliasing")
                .addCompilerFlag("-funswitch-loops")
                .addCompilerFlag("-finline-limit=300"))

        // Additional debug flags
        .addCompilationModeFlags(
            CompilationModeFlags.newBuilder()
                .setMode(CompilationMode.DBG)
                .addCompilerFlag("-O0")
                .addCompilerFlag("-UNDEBUG")
                .addCompilerFlag("-fno-omit-frame-pointer")
                .addCompilerFlag("-fno-strict-aliasing"));
  }

  private CToolchain.Builder createAarch64ClangToolchain(String clangVersion) {

    String toolchainName = "aarch64-linux-android-4.9";
    String targetPlatform = "aarch64-linux-android";
    String gccToolchain = ndkPaths.createGccToolchainPath(toolchainName);
    String llvmTriple = "aarch64-none-linux-android";

    return CToolchain.newBuilder()
        .setToolchainIdentifier("aarch64-linux-android-clang" + clangVersion)
        .setTargetSystemName("aarch64-linux-android")
        .setTargetCpu("arm64-v8a")
        .setCompiler("gcc-4.9")

        .addAllToolPath(ndkPaths.createClangToolpaths(toolchainName, targetPlatform, clangVersion))

        .addAllCxxBuiltinIncludeDirectory(
            ndkPaths.createToolchainIncludePaths(toolchainName, targetPlatform, "4.9"))

        .setBuiltinSysroot(ndkPaths.createBuiltinSysroot("arm64"))

        .setSupportsEmbeddedRuntimes(true)
        .setStaticRuntimesFilegroup("static-runtime-libs-" + toolchainName)
        .setDynamicRuntimesFilegroup("dynamic-runtime-libs-" + toolchainName)

        // Compiler flags
        .addCompilerFlag("-gcc-toolchain")
        .addCompilerFlag(gccToolchain)
        .addCompilerFlag("-target")
        .addCompilerFlag(llvmTriple)
        .addCompilerFlag("-ffunction-sections")
        .addCompilerFlag("-funwind-tables")
        .addCompilerFlag("-fstack-protector-strong")
        .addCompilerFlag("-fpic")
        .addCompilerFlag("-Wno-invalid-command-line-argument")
        .addCompilerFlag("-Wno-unused-command-line-argument")
        .addCompilerFlag("-no-canonical-prefixes")

        // Linker flags
        .addLinkerFlag("-gcc-toolchain")
        .addLinkerFlag(gccToolchain)
        .addLinkerFlag("-target")
        .addLinkerFlag(llvmTriple)
        .addLinkerFlag("-no-canonical-prefixes")

        // Additional release flags
        .addCompilationModeFlags(
            CompilationModeFlags.newBuilder()
                .setMode(CompilationMode.OPT)
                .addCompilerFlag("-O2")
                .addCompilerFlag("-g")
                .addCompilerFlag("-DNDEBUG")
                .addCompilerFlag("-fomit-frame-pointer")
                .addCompilerFlag("-fstrict-aliasing"))

        // Additional debug flags
        .addCompilationModeFlags(
            CompilationModeFlags.newBuilder()
                .setMode(CompilationMode.DBG)
                .addCompilerFlag("-O0")
                .addCompilerFlag("-UNDEBUG")
                .addCompilerFlag("-fno-omit-frame-pointer")
                .addCompilerFlag("-fno-strict-aliasing"));
  }

  private void createArmeabiToolchain(ImmutableList.Builder<CToolchain.Builder> builder,
      String gccVersion, String stackProtectorFlag, boolean thumb,
      CppConfiguration.Tool... excludedTools) {

    builder.add(createBaseArmeabiToolchain(thumb, gccVersion, stackProtectorFlag, excludedTools)
        .setToolchainIdentifier(
            createArmeabiName("arm-linux-androideabi-%s", gccVersion, thumb))
        .setTargetCpu("armeabi")

        .addCompilerFlag("-march=armv5te")
        .addCompilerFlag("-mtune=xscale")
        .addCompilerFlag("-msoft-float"));

    builder.add(createBaseArmeabiToolchain(thumb, gccVersion, stackProtectorFlag, excludedTools)
        .setToolchainIdentifier(
            createArmeabiName("arm-linux-androideabi-%s-v7a", gccVersion, thumb))
        .setTargetCpu("armeabi-v7a")

        .addCompilerFlag("-march=armv7-a") 
        .addCompilerFlag("-mfpu=vfpv3-d16")
        .addCompilerFlag("-mfloat-abi=softfp")

        .addLinkerFlag("-march=armv7-a")
        .addLinkerFlag("-Wl,--fix-cortex-a8"));

    builder.add(createBaseArmeabiToolchain(thumb, gccVersion, stackProtectorFlag, excludedTools)
        .setToolchainIdentifier(
            createArmeabiName("arm-linux-androideabi-%s-v7a-hard", gccVersion, thumb))
        .setTargetCpu("armeabi-v7a-hard")

        .addCompilerFlag("-march=armv7-a") 
        .addCompilerFlag("-mfpu=vfpv3-d16")
        .addCompilerFlag("-mhard-float")
        .addCompilerFlag("-D_NDK_MATH_NO_SOFTFP=1")

        .addLinkerFlag("-march=armv7-a")
        .addLinkerFlag("-Wl,--fix-cortex-a8")
        .addLinkerFlag("-Wl,--no-warn-mismatch")
        .addLinkerFlag("-lm_hard"));
  }

  private String createArmeabiName(String base, String gccVersion, boolean thumb) {
    String thumbString = thumb ? "-thumb" : "";
    return String.format(base, gccVersion) + thumbString; 
  }

  /**
   * Flags common to arm-linux-androideabi*
   */
  private CToolchain.Builder createBaseArmeabiToolchain(
      boolean thumb, String gccVersion, String stackProtectorFlag,
      CppConfiguration.Tool... excludedTools) {

    String toolchainName = "arm-linux-androideabi-" + gccVersion;
    String targetPlatform = "arm-linux-androideabi";

    ImmutableList<ToolPath> toolPaths = ndkPaths.createToolpaths(
        toolchainName,
        targetPlatform,
        excludedTools);

    ImmutableList<String> toolchainIncludes = ndkPaths.createToolchainIncludePaths(
        toolchainName,
        targetPlatform,
        gccVersion);

    CToolchain.Builder builder =
        CToolchain.newBuilder()
            .setTargetSystemName(targetPlatform)
            .setCompiler("gcc-" + gccVersion)

            .addAllToolPath(toolPaths)
            .addAllCxxBuiltinIncludeDirectory(toolchainIncludes)
            .setBuiltinSysroot(ndkPaths.createBuiltinSysroot("arm"))

            .setSupportsEmbeddedRuntimes(true)
            .setStaticRuntimesFilegroup("static-runtime-libs-" + toolchainName)
            .setDynamicRuntimesFilegroup("dynamic-runtime-libs-" + toolchainName)

            .addCompilerFlag(stackProtectorFlag)

            // Compiler flags
            .addCompilerFlag("-fpic")
            .addCompilerFlag("-ffunction-sections")
            .addCompilerFlag("-funwind-tables")
            .addCompilerFlag("-no-canonical-prefixes")

            // Linker flags
            .addLinkerFlag("-no-canonical-prefixes");

    if (thumb) {
      builder.addCompilationModeFlags(CompilationModeFlags.newBuilder()
          .setMode(CompilationMode.OPT)
          .addCompilerFlag("-mthumb")
          .addCompilerFlag("-Os")
          .addCompilerFlag("-g")
          .addCompilerFlag("-DNDEBUG")
          .addCompilerFlag("-fomit-frame-pointer")
          .addCompilerFlag("-fno-strict-aliasing")
          .addCompilerFlag("-finline-limit=64"));

      builder.addCompilationModeFlags(CompilationModeFlags.newBuilder()
          .setMode(CompilationMode.DBG)
          .addCompilerFlag("-g")
          .addCompilerFlag("-fno-strict-aliasing")
          .addCompilerFlag("-finline-limit=64")
          .addCompilerFlag("-O0")
          .addCompilerFlag("-UNDEBUG")
          .addCompilerFlag("-marm")
          .addCompilerFlag("-fno-omit-frame-pointer"));
    } else {
      builder.addCompilationModeFlags(CompilationModeFlags.newBuilder()
          .setMode(CompilationMode.OPT)
          .addCompilerFlag("-O2")
          .addCompilerFlag("-g")
          .addCompilerFlag("-DNDEBUG")
          .addCompilerFlag("-fomit-frame-pointer")
          .addCompilerFlag("-fstrict-aliasing")
          .addCompilerFlag("-funswitch-loops")
          .addCompilerFlag("-finline-limit=300"));

      builder.addCompilationModeFlags(CompilationModeFlags.newBuilder()
          .setMode(CompilationMode.DBG)
          .addCompilerFlag("-g")
          .addCompilerFlag("-funswitch-loops")
          .addCompilerFlag("-finline-limit=300")
          .addCompilerFlag("-O0")
          .addCompilerFlag("-UNDEBUG")
          .addCompilerFlag("-fno-omit-frame-pointer")
          .addCompilerFlag("-fno-strict-aliasing"));
    }

    return builder;
  }

  private void createArmeabiClangToolchain(ImmutableList.Builder<CToolchain.Builder> builder,
      String clangVersion, boolean thumb) {
   
    builder.add(createBaseArmeabiClangToolchain(clangVersion, thumb)
        .setToolchainIdentifier(
            createArmeabiName("arm-linux-androideabi-clang%s", clangVersion, thumb))
        .setTargetCpu("armeabi")

        .addCompilerFlag("-target")
        .addCompilerFlag("armv5te-none-linux-androideabi") // LLVM_TRIPLE
        .addCompilerFlag("-march=armv5te")
        .addCompilerFlag("-mtune=xscale")
        .addCompilerFlag("-msoft-float")

        .addLinkerFlag("-target")
        // LLVM_TRIPLE
        .addLinkerFlag("armv5te-none-linux-androideabi"));

    builder.add(createBaseArmeabiClangToolchain(clangVersion, thumb)
        .setToolchainIdentifier(
            createArmeabiName("arm-linux-androideabi-clang%s-v7a", clangVersion, thumb))
        .setTargetCpu("armeabi-v7a")

        .addCompilerFlag("-target")
        .addCompilerFlag("armv7-none-linux-androideabi") // LLVM_TRIPLE
        .addCompilerFlag("-march=armv7-a") 
        .addCompilerFlag("-mfloat-abi=softfp")
        .addCompilerFlag("-mfpu=vfpv3-d16")

        .addLinkerFlag("-target")
        .addLinkerFlag("armv7-none-linux-androideabi") // LLVM_TRIPLE
        .addLinkerFlag("-Wl,--fix-cortex-a8"));

    builder.add(createBaseArmeabiClangToolchain(clangVersion, thumb)
        .setToolchainIdentifier(
            createArmeabiName("arm-linux-androideabi-clang%s-v7a-hard", clangVersion, thumb))
        .setTargetCpu("armeabi-v7a-hard")

        .addCompilerFlag("-target")
        .addCompilerFlag("armv7-none-linux-androideabi") // LLVM_TRIPLE
        .addCompilerFlag("-march=armv7-a") 
        .addCompilerFlag("-mfpu=vfpv3-d16")
        .addCompilerFlag("-mhard-float")
        .addCompilerFlag("-D_NDK_MATH_NO_SOFTFP=1")

        .addLinkerFlag("-target")
        .addLinkerFlag("armv7-none-linux-androideabi") // LLVM_TRIPLE
        .addLinkerFlag("-Wl,--fix-cortex-a8")
        .addLinkerFlag("-Wl,--no-warn-mismatch")
        .addLinkerFlag("-lm_hard"));
  }

  private CToolchain.Builder createBaseArmeabiClangToolchain(String clangVersion, boolean thumb) {

    String toolchainName = "arm-linux-androideabi-4.8";
    String targetPlatform = "arm-linux-androideabi";
    String gccToolchain = ndkPaths.createGccToolchainPath("arm-linux-androideabi-4.8");

    CToolchain.Builder builder =
        CToolchain.newBuilder()
            .setTargetSystemName("arm-linux-androideabi")
            .setCompiler("gcc-4.8")

            .addAllToolPath(
                ndkPaths.createClangToolpaths(
                    toolchainName,
                    targetPlatform,
                    clangVersion,
                    // gcc-4.8 arm doesn't have gcov-tool
                    CppConfiguration.Tool.GCOVTOOL))

            .addAllCxxBuiltinIncludeDirectory(
                ndkPaths.createToolchainIncludePaths(toolchainName, targetPlatform, "4.8"))

            .setBuiltinSysroot(ndkPaths.createBuiltinSysroot("arm"))

            .setSupportsEmbeddedRuntimes(true)
            .setStaticRuntimesFilegroup("static-runtime-libs-" + toolchainName)
            .setDynamicRuntimesFilegroup("dynamic-runtime-libs-" + toolchainName)

            // Compiler flags
            .addCompilerFlag("-gcc-toolchain")
            .addCompilerFlag(gccToolchain)
            .addCompilerFlag("-fpic")
            .addCompilerFlag("-ffunction-sections")
            .addCompilerFlag("-funwind-tables")
            .addCompilerFlag("-fstack-protector-strong")
            .addCompilerFlag("-Wno-invalid-command-line-argument")
            .addCompilerFlag("-Wno-unused-command-line-argument")
            .addCompilerFlag("-no-canonical-prefixes")
            .addCompilerFlag("-fno-integrated-as")

            // Linker flags
            .addLinkerFlag("-gcc-toolchain")
            .addLinkerFlag(gccToolchain)
            .addLinkerFlag("-no-canonical-prefixes");

    if (thumb) {
      builder.addCompilationModeFlags(CompilationModeFlags.newBuilder()
          .setMode(CompilationMode.OPT)
          .addCompilerFlag("-mthumb")
          .addCompilerFlag("-Os")
          .addCompilerFlag("-g")
          .addCompilerFlag("-DNDEBUG")
          .addCompilerFlag("-fomit-frame-pointer")
          .addCompilerFlag("-fno-strict-aliasing"));

      builder.addCompilationModeFlags(CompilationModeFlags.newBuilder()
          .setMode(CompilationMode.DBG)
          .addCompilerFlag("-g")
          .addCompilerFlag("-fno-strict-aliasing")
          .addCompilerFlag("-O0")
          .addCompilerFlag("-UNDEBUG")
          .addCompilerFlag("-marm")
          .addCompilerFlag("-fno-omit-frame-pointer"));
    } else {
      builder.addCompilationModeFlags(CompilationModeFlags.newBuilder()
          .setMode(CompilationMode.OPT)
          .addCompilerFlag("-O2")
          .addCompilerFlag("-g")
          .addCompilerFlag("-DNDEBUG")
          .addCompilerFlag("-fomit-frame-pointer")
          .addCompilerFlag("-fstrict-aliasing"));

      builder.addCompilationModeFlags(CompilationModeFlags.newBuilder()
          .setMode(CompilationMode.DBG)
          .addCompilerFlag("-g")
          .addCompilerFlag("-O0")
          .addCompilerFlag("-UNDEBUG")
          .addCompilerFlag("-fno-omit-frame-pointer")
          .addCompilerFlag("-fno-strict-aliasing"));
    }
    
    return builder;
  }
}