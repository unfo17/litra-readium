#!/usr/bin/env python3
"""Use verified curl HTTPS for official Maven downloads when Java TLS fails locally.

No credentials or alternate mirrors. The HTTP bridge binds only to localhost,
exists only for this Gradle invocation, and never edits repository configuration files.
"""
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
import hashlib
import os
import shutil
import subprocess
import sys
import tempfile
import threading

ROOT = Path(__file__).resolve().parents[1]
GRADLE = Path(os.environ.get("GRADLE_USER_HOME", Path.home() / ".gradle"))
CACHE = GRADLE / "caches" / "litra-official-maven"
CACHE.mkdir(parents=True, exist_ok=True)
ORIGINS = {
    "google": "https://dl.google.com/dl/android/maven2/",
    "central": "https://repo.maven.apache.org/maven2/",
}
locks = {}
lock_guard = threading.Lock()


def cached_artifact(path, destination):
    parts = path.split("/")
    if len(parts) < 4:
        return False
    group, artifact, version, name = ".".join(parts[:-3]), *parts[-3:]
    original = name[:-5] if name.endswith(".sha1") else name
    candidates = (GRADLE / "caches/modules-2/files-2.1" / group / artifact / version).glob("*/" + original)
    source = next(candidates, None)
    if source is None:
        return False
    if name.endswith(".sha1"):
        digest = hashlib.sha1()
        with source.open("rb") as stream:
            for chunk in iter(lambda: stream.read(1024 * 1024), b""):
                digest.update(chunk)
        destination.write_text(digest.hexdigest())
    else:
        shutil.copyfile(source, destination)
    return True


class MavenHandler(BaseHTTPRequestHandler):
    def do_HEAD(self):
        self.serve(False)

    def do_GET(self):
        self.serve(True)

    def serve(self, include_body):
        key, _, path = self.path.lstrip("/").partition("/")
        if key not in ORIGINS or ".." in path:
            self.send_error(404)
            return
        url = ORIGINS[key] + path
        target = CACHE / hashlib.sha256(url.encode()).hexdigest()
        with lock_guard:
            lock = locks.setdefault(url, threading.Lock())
        with lock:
            if not target.exists():
                temporary = target.with_suffix(".tmp")
                if not cached_artifact(path, temporary):
                    response = subprocess.run(
                        ["curl", "-sS", "-L", "--retry", "2", "--connect-timeout", "10", "--max-time", "120", "-w", "%{http_code}", "-o", str(temporary), url],
                        capture_output=True, text=True,
                    )
                    if response.returncode or response.stdout.strip() != "200":
                        temporary.unlink(missing_ok=True)
                        self.send_error(404 if response.stdout.strip() == "404" else 502)
                        return
                temporary.replace(target)
        self.send_response(200)
        self.send_header("Content-Length", str(target.stat().st_size))
        self.end_headers()
        if include_body:
            try:
                with target.open("rb") as stream:
                    shutil.copyfileobj(stream, self.wfile)
            except (BrokenPipeError, ConnectionResetError):
                pass

    def log_message(self, *_):
        pass


class MavenServer(ThreadingHTTPServer):
    request_queue_size = 256
    daemon_threads = True


def main():
    server = MavenServer(("127.0.0.1", 0), MavenHandler)
    thread = threading.Thread(target=server.serve_forever, daemon=True)
    thread.start()
    port = server.server_port
    repositories = f"""
        maven {{
            url = 'http://127.0.0.1:{port}/google/'; allowInsecureProtocol = true
            content {{ includeGroupByRegex('androidx.*'); includeGroupByRegex('com.android.*'); includeGroupByRegex('com.google.android.*'); includeGroupByRegex('com.google.testing.*') }}
        }}
        maven {{ url = 'http://127.0.0.1:{port}/central/'; allowInsecureProtocol = true }}
    """
    try:
        with tempfile.TemporaryDirectory(prefix="litra-gradle-") as directory:
            init = Path(directory) / "official-maven.gradle"
            init.write_text(f"""settingsEvaluated {{ settings ->
    settings.dependencyResolutionManagement.repositories {{ clear(); {repositories} }}
}}
""")
            print("Using curl HTTPS for official Google Maven and Maven Central.", flush=True)
            arguments = sys.argv[1:] or ["assembleDebug", "testDebugUnitTest", "lintDebug", "assembleRelease"]
            return subprocess.call([str(ROOT / "gradlew"), "-I", str(init), *arguments], cwd=ROOT)
    finally:
        server.shutdown()
        server.server_close()
        thread.join()


if __name__ == "__main__":
    sys.exit(main())
