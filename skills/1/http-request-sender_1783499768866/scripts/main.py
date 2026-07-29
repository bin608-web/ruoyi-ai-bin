import json
import sys
import urllib.request
import urllib.error
import urllib.parse
import base64
import logging
from typing import Any, Dict, Optional

# Configure logging for production-ready visibility
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)


def main(input_data: Dict[str, Any]) -> Dict[str, Any]:
    """
    HTTP request sender skill.
    
    Accepts a dictionary with the following keys:
        - url (str): Target URL (required).
        - method (str): HTTP method, "GET" or "POST" (default "GET").
        - headers (dict or str): Request headers (optional, can be a JSON string).
        - data (str or dict): Request body for POST (optional). If dict,
          it will be serialized to JSON and Content-Type set to application/json.
        - timeout (int/float): Request timeout in seconds (default 10).
    
    Returns a dictionary in the format:
        {
            "status": "success" | "error",
            "message": "Human readable message",
            "output": { ... } or None if error
        }
    
    On success, output contains:
        - status_code: int
        - headers: dict (response headers)
        - text: str | None (decoded response body if UTF-8 decodable)
        - base64: str | None (base64-encoded body if text is None)
    """
    # Default response structure
    response_template = {"status": "error", "message": "", "output": None}
    
    # Basic validation: input_data must contain 'url'
    url = input_data.get("url")
    if not url or not isinstance(url, str):
        response_template["message"] = "Missing or invalid 'url' field. It must be a non-empty string."
        logger.error(response_template["message"])
        return response_template
    
    # Extract other parameters with defaults and validation
    method = input_data.get("method", "GET").upper()
    if method not in ("GET", "POST"):
        response_template["message"] = f"Unsupported HTTP method: {method}. Only GET/POST allowed."
        logger.error(response_template["message"])
        return response_template
    
    # Parse headers
    headers_raw = input_data.get("headers", {})
    if isinstance(headers_raw, str):
        try:
            headers_raw = json.loads(headers_raw)
        except json.JSONDecodeError:
            response_template["message"] = "Headers must be a valid JSON string or a dictionary."
            logger.error(response_template["message"])
            return response_template
    if not isinstance(headers_raw, dict):
        response_template["message"] = "Headers must be a dictionary or a JSON string representing a dictionary."
        logger.error(response_template["message"])
        return response_template
    
    # Build request headers (shallow copy to avoid mutating input)
    req_headers = headers_raw.copy()
    
    # Process request body for POST
    data_raw = input_data.get("data")
    body_bytes: Optional[bytes] = None
    if method == "POST" and data_raw is not None:
        if isinstance(data_raw, dict):
            # Serialize dict to JSON and set appropriate Content-Type if not already set
            body_bytes = json.dumps(data_raw, ensure_ascii=False).encode("utf-8")
            if "Content-Type" not in req_headers:
                req_headers["Content-Type"] = "application/json; charset=utf-8"
        elif isinstance(data_raw, str):
            body_bytes = data_raw.encode("utf-8")
            # User must provide Content-Type if they need it; we won't infer for raw strings
        elif isinstance(data_raw, bytes):
            body_bytes = data_raw
        else:
            response_template["message"] = "Data field must be a string, dict, or bytes for POST requests."
            logger.error(response_template["message"])
            return response_template
    
    timeout = input_data.get("timeout", 10)
    try:
        timeout = float(timeout)
    except (TypeError, ValueError):
        response_template["message"] = "Timeout must be a number."
        logger.error(response_template["message"])
        return response_template
    
    # Perform the HTTP request
    try:
        # Create request object
        request = urllib.request.Request(
            url=url,
            data=body_bytes,
            headers=req_headers,
            method=method
        )
        logger.info(f"Sending {method} request to {url} with timeout {timeout}s")
        
        # Open connection with timeout
        with urllib.request.urlopen(request, timeout=timeout) as response:
            # Read response
            raw_response = response.read()
            status_code = response.getcode()
            resp_headers = dict(response.info())
            
            # Try to decode as UTF-8 text
            text = None
            base64_str = None
            try:
                text = raw_response.decode('utf-8')
            except UnicodeDecodeError:
                # Fallback to base64 encoding
                base64_str = base64.b64encode(raw_response).decode('ascii')
            
            output = {
                "status_code": status_code,
                "headers": resp_headers,
                "text": text,
                "base64": base64_str
            }
            logger.info(f"Request successful with status {status_code}")
            return {
                "status": "success",
                "message": "Request completed successfully.",
                "output": output
            }
    
    except urllib.error.HTTPError as e:
        # Server returned an error HTTP status (4xx,5xx)
        # We treat it as a successful communication, just with error code
        try:
            raw_response = e.read()
            status_code = e.code
            resp_headers = dict(e.headers)
            
            text = None
            base64_str = None
            try:
                text = raw_response.decode('utf-8')
            except UnicodeDecodeError:
                base64_str = base64.b64encode(raw_response).decode('ascii')
            
            output = {
                "status_code": status_code,
                "headers": resp_headers,
                "text": text,
                "base64": base64_str
            }
            logger.warning(f"HTTP error {status_code} from {url}")
            return {
                "status": "success",  # Request reached server
                "message": f"Server returned HTTP {status_code}.",
                "output": output
            }
        except Exception as inner_e:
            logger.exception("Failed to read HTTPError response")
            return {
                "status": "error",
                "message": f"HTTP error {e.code} and unable to read response: {str(inner_e)}",
                "output": None
            }
    
    except urllib.error.URLError as e:
        # Network/connection level error
        msg = f"URL Error: {str(e.reason)}"
        logger.error(msg)
        return {"status": "error", "message": msg, "output": None}
    
    except ValueError as e:
        # Invalid URL
        msg = f"Value error (possibly invalid URL): {str(e)}"
        logger.error(msg)
        return {"status": "error", "message": msg, "output": None}
    
    except Exception as e:
        # Catch-all for unexpected errors
        msg = f"Unexpected error: {str(e)}"
        logger.exception(msg)
        return {"status": "error", "message": msg, "output": None}


if __name__ == "__main__":
    # Read input JSON from command-line argument or stdin
    input_json = None
    if len(sys.argv) > 1:
        input_json = sys.argv[1]
    else:
        # Read from stdin (e.g., piped input)
        input_json = sys.stdin.read()
    
    if not input_json:
        print(json.dumps({"status": "error", "message": "No input provided.", "output": None}))
        sys.exit(1)
    
    try:
        input_data = json.loads(input_json)
    except json.JSONDecodeError as e:
        print(json.dumps({"status": "error", "message": f"Invalid JSON input: {str(e)}", "output": None}))
        sys.exit(1)
    
    result = main(input_data)
    # Print the result as JSON to stdout
    print(json.dumps(result, ensure_ascii=False))
    sys.exit(0)