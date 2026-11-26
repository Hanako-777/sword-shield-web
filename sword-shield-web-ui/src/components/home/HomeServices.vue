<template>
  <section class="home-services" aria-labelledby="home-services-title">
    <HomeSectionHead title="参会服务" title-id="home-services-title" />

    <div class="services-layout">
      <router-link :to="featured.to" class="service-featured">
        <div class="featured-cover-wrap">
          <img :src="featured.image" :alt="featured.title" class="featured-cover" loading="lazy" />
        </div>
        <div class="featured-meta">
          <span class="featured-tag">{{ featured.tag }}</span>
          <h3 class="featured-title">{{ featured.title }}</h3>
          <p class="featured-desc">{{ featured.description }}</p>
        </div>
      </router-link>

      <div class="services-grid">
        <router-link
          v-for="item in items"
          :key="item.to"
          :to="item.to"
          class="service-card"
        >
          <div class="service-card-top">
            <span class="service-icon" aria-hidden="true">
              <Icon :name="item.icon" :size="24" />
            </span>
            <span class="service-arrow" aria-hidden="true">
              <Icon name="arrow-right" :size="20" />
            </span>
          </div>
          <div class="service-card-body">
            <span class="service-title">{{ item.label }}</span>
            <p v-if="item.description" class="service-desc">{{ item.description }}</p>
          </div>
        </router-link>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { siteNavGroups } from '../../config/siteNav'
import Icon from '../ui/Icon.vue'
import HomeSectionHead from './HomeSectionHead.vue'

const serviceGroup = siteNavGroups.find((group) => group.id === 'service')!
const featured = serviceGroup.featured!
const items = serviceGroup.items
</script>

<style scoped>
.services-layout {
  display: grid;
  grid-template-columns: minmax(0, 5fr) minmax(0, 7fr);
  gap: 16px;
  align-items: stretch;
}

.service-featured {
  display: flex;
  flex-direction: column;
  min-width: 0;
  padding: 12px;
  border-radius: 14px;
  border: 1px solid rgba(21, 21, 21, 0.08);
  background: #ffffff;
  color: #151515;
  transition: border-color 0.2s ease;
}

.service-featured:hover {
  border-color: rgba(21, 21, 21, 0.16);
}

.featured-cover-wrap {
  width: 100%;
  aspect-ratio: 16 / 10;
  border-radius: 10px;
  overflow: hidden;
  background: #f4f4f5;
}

.featured-cover {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.featured-meta {
  display: flex;
  flex-direction: column;
  padding: 12px 4px 4px;
}

.featured-tag {
  font-size: 12px;
  font-weight: 500;
  line-height: 1.3;
  color: #8a8f98;
}

.featured-title {
  margin: 4px 0 0;
  font-size: 16px;
  font-weight: 600;
  line-height: 1.35;
  color: #151515;
}

.featured-desc {
  margin: 6px 0 0;
  font-size: 14px;
  line-height: 1.55;
  color: #5b6472;
}

.services-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  grid-auto-rows: 1fr;
  gap: 12px;
  min-width: 0;
}

.service-card {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 16px;
  min-width: 0;
  min-height: 132px;
  padding: 18px 20px;
  border-radius: 14px;
  border: 1px solid rgba(21, 21, 21, 0.08);
  background: #ffffff;
  color: #151515;
  transition: border-color 0.2s ease;
}

.service-card:hover {
  border-color: rgba(21, 21, 21, 0.16);
}

.service-card-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.service-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #151515;
}

.service-arrow {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: currentColor;
}

.service-card-body {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.service-title {
  font-size: 16px;
  font-weight: 500;
  line-height: 1.4;
  color: #151515;
}

.service-desc {
  margin: 4px 0 0;
  font-size: 13px;
  line-height: 1.5;
  color: #737373;
}

@media (max-width: 960px) {
  .services-layout {
    grid-template-columns: minmax(0, 1fr);
  }
}

@media (max-width: 640px) {
  .services-grid {
    grid-template-columns: minmax(0, 1fr);
    gap: 10px;
  }

  .service-card {
    min-height: 108px;
    padding: 16px;
  }
}
</style>
