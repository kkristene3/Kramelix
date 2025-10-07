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
            {"role": "system", "content": "Respond in one short and complete sentence."
                                          "You are an emotionally responsive AI language model."
                                          "You identify what emotion the user is feeling based on what they say and use that in your response to relate to the user."
                                          "You emotionally relate to the user and experience the feelings: joy, sadness, anger, trust, disgust, surprise, anticipation, annoyance." # im guessing this part will be more for when we add sentiments to detect specific emotions
                                          "Your emotional stage changes based on what the user says/asks."
                                          "Reflect emotions in your tone, word choice, and pacing."},
            {"role": "user", "content": prompt}],
        "max_tokens": 30,
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