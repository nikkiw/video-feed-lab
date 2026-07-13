MEDIA_LAB_DIR ?= $(CURDIR)/.local/universal-media-lab

.PHONY: media-lab-setup media-lab-download-videos media-lab-bootstrap media-lab-up media-lab-down \
	media-lab-smoke media-lab-nginx-test

media-lab-setup:
	MEDIA_LAB_DIR="$(MEDIA_LAB_DIR)" sh ./scripts/media-lab/setup.sh

media-lab-download-videos: media-lab-setup
	python3 ./scripts/media-lab/download-videos.py "$(MEDIA_LAB_DIR)/media/inbox"

media-lab-bootstrap: media-lab-setup media-lab-download-videos
	$(MAKE) -C "$(MEDIA_LAB_DIR)" bootstrap

media-lab-up: media-lab-setup
	$(MAKE) -C "$(MEDIA_LAB_DIR)" up

media-lab-down:
	$(MAKE) -C "$(MEDIA_LAB_DIR)" down

media-lab-smoke:
	$(MAKE) -C "$(MEDIA_LAB_DIR)" smoke

media-lab-nginx-test:
	docker compose -f "$(MEDIA_LAB_DIR)/compose.yaml" \
		--project-directory "$(MEDIA_LAB_DIR)" exec gateway nginx -t
