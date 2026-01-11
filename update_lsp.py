#!/usr/bin/env python3
import shutil
import textwrap
import os
import subprocess
from functools import cache
import re


def run_cmd(args: list[str], working_dir: str = ".") -> str:
    print(f"\tRunning command: {' '.join(args)}")
    result = subprocess.run(args, cwd=working_dir, capture_output=True, text=True, check=True)
    if result.returncode != 0:
        print(f"Command failed with exit code {result.returncode}: {result.args}")
        os.exit(1)
    return result.stdout

@cache
def get_repo_path() -> str:
    cache_file = "openvadl-path.txt"
    if not os.path.exists(cache_file):
        repo_path = input("Enter the path to the OpenVADL repo:")
        with open(cache_file, "w") as f:
            f.write(repo_path)

    with open(cache_file) as f:
        return f.read()


def build_lsp():
    print("1) Building LSP")
    repo_path = get_repo_path()

    ## Checkout master
    current_branch = run_cmd(["git", "rev-parse", "--abbrev-ref", "HEAD"], repo_path).strip()
    if current_branch != "master":
        print(f"Not on master branch ({current_branch}).")
        answer = input("Can we switch to master? (y/n): ")
        if answer.lower() not in ["y", "yes"]:
            print("Aborting LSP build.")
            return

    run_cmd(["git", "checkout", "master"], repo_path)
    run_cmd(["git", "pull",], repo_path)

    ## Build LSP
    run_cmd(["./gradlew", ":vadl-lsp:jlink"], repo_path)


def copy_lsp():
    print("2) Copy LSP results")
    repo_path = get_repo_path()
    target_dir = "src/main/resources/openvadl-lsp"

    if os.path.exists(target_dir):
        shutil.rmtree(target_dir)

    shutil.copytree(repo_path + "/vadl-lsp/build/image", target_dir)


def update_metadata():
    print("3) Update Metadata")

    # Read build.gradle.kts
    build_file = "build.gradle.kts"
    with open(build_file, 'r') as f:
        content = f.read()

    # Find and increment the version
    version_pattern = r'version = "(\d+)\.(\d+)\.(\d+)"'
    match = re.search(version_pattern, content)

    if match:
        major, minor, patch = match.groups()
        new_patch = int(patch) + 1
        new_version = f"{major}.{minor}.{new_patch}"

        # Replace the version
        new_content = re.sub(version_pattern, f'version = "{new_version}"', content)

        with open(build_file, 'w') as f:
            f.write(new_content)

        print(f"\tUpdated version from {major}.{minor}.{patch} to {new_version}")
    else:
        print("\tWarning: Could not find version in build.gradle.kts")


def build_plugin():
    print("4) Building plugin")
    run_cmd(["./gradlew", "buildPlugin"])



def publish():
    print(textwrap.dedent("""
    5) Publishing LSP
    At the moment we are still awaiting approval from JetBrains. So all updates have to be done manually.
    https://plugins.jetbrains.com/plugin/29659-openvadl/edit
    
    Build plugin is at:
    build/distributions/
    """).strip())

def main():
    build_lsp()
    print()
    copy_lsp()
    print()
    update_metadata()
    print()
    build_plugin()
    print()
    publish()
    print()
    print("Done ✨")

if __name__ == "__main__":
    main()