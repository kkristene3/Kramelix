import httpx
import json

def test_function(num):
    print("Testing python addition")
    print("alex is not the best")
    return num+1

def chat(api_key, prompt):
    openAI_URL = "https://api.openai.com/v1/chat/completions"
    headers = {
        "Authorization": f"Bearer {api_key}",
        "Content-Type": "application/json"
    }

    # TODO: update the payload data when we need to
    payload = {
        "model": "gpt-4.1-mini",
        "messages": [
            {"role": "system", "content": "You only respond in 4 word poems"},
            {"role": "user", "content": prompt}],
        "max_tokens": 7,
        "temperature": 0.7
    }

    try:
        response = httpx.post(openAI_URL, headers=headers, json=payload, timeout=60)
        response.raise_for_status()
        data = response.json()
        print("AI response:", data["choices"][0]["message"]["content"])
        return data["choices"][0]["message"]["content"]
    except Exception as e:
        print(f"Error: {e}")
        return e