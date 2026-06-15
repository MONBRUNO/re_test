import json
import threading
import urllib.parse
import urllib.request
import webbrowser
from http.server import BaseHTTPRequestHandler, HTTPServer


SCOPE = "https://www.googleapis.com/auth/gmail.send"
REDIRECT_URI = "http://localhost:8080/oauth2callback"


class OAuthCallbackHandler(BaseHTTPRequestHandler):
    code = None
    error = None

    def do_GET(self):
        parsed = urllib.parse.urlparse(self.path)
        params = urllib.parse.parse_qs(parsed.query)
        OAuthCallbackHandler.code = params.get("code", [None])[0]
        OAuthCallbackHandler.error = params.get("error", [None])[0]

        self.send_response(200)
        self.send_header("Content-Type", "text/html; charset=utf-8")
        self.end_headers()
        self.wfile.write(
            "인증이 끝났습니다. 이 브라우저 창은 닫아도 됩니다.".encode("utf-8")
        )

    def log_message(self, format, *args):
        return


def exchange_code_for_tokens(client_id, client_secret, code):
    data = urllib.parse.urlencode(
        {
            "client_id": client_id,
            "client_secret": client_secret,
            "code": code,
            "grant_type": "authorization_code",
            "redirect_uri": REDIRECT_URI,
        }
    ).encode("utf-8")

    request = urllib.request.Request(
        "https://oauth2.googleapis.com/token",
        data=data,
        headers={"Content-Type": "application/x-www-form-urlencoded"},
        method="POST",
    )
    with urllib.request.urlopen(request, timeout=20) as response:
        return json.loads(response.read().decode("utf-8"))


def main():
    print("Google Cloud에서 발급받은 Desktop app OAuth 값을 입력하세요.")
    client_id = input("Client ID: ").strip()
    client_secret = input("Client Secret: ").strip()

    query = urllib.parse.urlencode(
        {
            "client_id": client_id,
            "redirect_uri": REDIRECT_URI,
            "response_type": "code",
            "scope": SCOPE,
            "access_type": "offline",
            "prompt": "consent",
        }
    )
    auth_url = "https://accounts.google.com/o/oauth2/v2/auth?" + query

    server = HTTPServer(("localhost", 8080), OAuthCallbackHandler)
    thread = threading.Thread(target=server.handle_request, daemon=True)
    thread.start()

    print("\n브라우저가 열리면 Gmail 계정으로 로그인하고 메일 보내기 권한을 허용하세요.")
    print(auth_url)
    webbrowser.open(auth_url)
    thread.join(timeout=180)
    server.server_close()

    if OAuthCallbackHandler.error:
        raise SystemExit("OAuth 실패: " + OAuthCallbackHandler.error)
    if not OAuthCallbackHandler.code:
        raise SystemExit("인증 코드를 받지 못했습니다. 다시 실행하세요.")

    tokens = exchange_code_for_tokens(
        client_id, client_secret, OAuthCallbackHandler.code
    )
    refresh_token = tokens.get("refresh_token")
    if not refresh_token:
        print(json.dumps(tokens, indent=2, ensure_ascii=False))
        raise SystemExit("refresh_token이 없습니다. OAuth 동의 화면에서 prompt=consent로 다시 허용해야 합니다.")

    print("\nRender 환경변수에 아래 값을 넣으세요.")
    print("GMAIL_API_CLIENT_ID=" + client_id)
    print("GMAIL_API_CLIENT_SECRET=" + client_secret)
    print("GMAIL_API_REFRESH_TOKEN=" + refresh_token)
    print("GMAIL_API_FROM=여기에_보내는_Gmail주소")


if __name__ == "__main__":
    main()
