#!/usr/bin/env python3
import os
import sys
import urllib.request
from concurrent.futures import ThreadPoolExecutor

# List of 20 high-quality and dynamic test videos from Pexels and Pixabay
VIDEOS = {
    "dance-neon.mp4": "https://videos.pexels.com/video-files/5536129/5536129-uhd_1440_2560_25fps.mp4",
    "girl-portrait.mp4": "https://videos.pexels.com/video-files/6706926/6706926-hd_1920_1080_25fps.mp4",
    "woman-smile.mp4": "https://videos.pexels.com/video-files/5495175/5495175-uhd_2732_1440_25fps.mp4",
    "man-running.mp4": "https://videos.pexels.com/video-files/4584807/4584807-uhd_2560_1440_25fps.mp4",
    "people-active.mp4": "https://videos.pexels.com/video-files/7976476/7976476-uhd_2732_1440_25fps.mp4",
    "couple-laugh.mp4": "https://videos.pexels.com/video-files/5496775/5496775-uhd_2560_1440_30fps.mp4",
    "woman-dancing.mp4": "https://videos.pexels.com/video-files/5981354/5981354-uhd_2732_1440_25fps.mp4",
    "urban-lights.mp4": "https://videos.pexels.com/video-files/6722759/6722759-uhd_2732_1440_25fps.mp4",
    "skater-jump.mp4": "https://videos.pexels.com/video-files/8410107/8410107-hd_1920_1080_25fps.mp4",
    "city-street.mp4": "https://videos.pexels.com/video-files/4494789/4494789-hd_1920_1080_30fps.mp4",
    "surf-ocean.mp4": "https://cdn.pixabay.com/video/2024/04/18/208442_large.mp4",
    "active-sports.mp4": "https://cdn.pixabay.com/video/2025/03/28/268290_large.mp4",
    "neon-signs.mp4": "https://cdn.pixabay.com/video/2024/05/23/213387_medium.mp4",
    "nature-waterfall.mp4": "https://cdn.pixabay.com/video/2020/04/24/37088-413229662_large.mp4",
    "urban-runner.mp4": "https://cdn.pixabay.com/video/2023/11/28/191159-889246512_tiny.mp4",
    "scenery-mountains.mp4": "https://cdn.pixabay.com/video/2024/03/31/206294_large.mp4",
    "dj-turntable.mp4": "https://cdn.pixabay.com/video/2023/06/07/166239-834228361_large.mp4",
    "cyberpunk-city.mp4": "https://cdn.pixabay.com/video/2023/06/20/168085-838533639_medium.mp4",
    "street-dance.mp4": "https://cdn.pixabay.com/video/2022/10/28/136829-764955767_medium.mp4",
    "aesthetic-sunset.mp4": "https://cdn.pixabay.com/video/2021/11/03/94468-643067856_medium.mp4"
}

def download_video(filename, url, dest_dir):
    dest_path = os.path.join(dest_dir, filename)
    headers = {"User-Agent": "Mozilla/5.0"}
    
    try:
        req = urllib.request.Request(url, headers=headers)
        
        # Check if file exists and content-length matches
        if os.path.exists(dest_path):
            try:
                # HEAD request to check length
                head_req = urllib.request.Request(url, headers=headers, method="HEAD")
                with urllib.request.urlopen(head_req) as head_resp:
                    content_length = int(head_resp.headers.get("Content-Length", 0))
                    if content_length > 0 and os.path.getsize(dest_path) == content_length:
                        print(f" -> Skipping already complete: {filename}")
                        return True
            except Exception:
                pass # Fallback to overwriting if HEAD check fails

        print(f" -> Downloading {filename} from {url}...")
        with urllib.request.urlopen(req) as resp, open(dest_path, "wb") as out_file:
            out_file.write(resp.read())
        print(f" -> Finished: {filename}")
        return True
    except Exception as e:
        print(f"ERROR downloading {filename}: {e}", file=sys.stderr)
        return False

def main():
    if len(sys.argv) > 1:
        dest_dir = sys.argv[1]
    else:
        # Default to relative location in workspace
        script_dir = os.path.dirname(os.path.abspath(__file__))
        dest_dir = os.path.join(script_dir, "..", "..", ".local", "universal-media-lab", "media", "inbox")
    
    dest_dir = os.path.abspath(dest_dir)
    print(f"Target download directory: {dest_dir}")
    os.makedirs(dest_dir, exist_ok=True)
    
    # Download concurrently using up to 5 workers
    with ThreadPoolExecutor(max_workers=5) as executor:
        futures = [
            executor.submit(download_video, filename, url, dest_dir)
            for filename, url in VIDEOS.items()
        ]
        results = [f.result() for f in futures]
        
    succeeded = sum(1 for r in results if r)
    print(f"\nCompleted: {succeeded}/{len(VIDEOS)} videos downloaded successfully.")
    
    if succeeded == len(VIDEOS):
        sys.exit(0)
    else:
        sys.exit(1)

if __name__ == "__main__":
    main()
