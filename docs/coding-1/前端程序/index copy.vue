<template>
  <div ref="wordCloud" class="word-cloud-container" :style="{width: width, height: height}" />
</template>

<script>
import * as echarts from 'echarts'
import 'echarts-wordcloud'

export default {
  name: 'WordCloud',
  props: {
    data: {
      type: Array,
      required: true,
      default: () => []
    },
    width: {
      type: String,
      default: '100%'
    },
    height: {
      type: String,
      default: '400px'
    },
    maxWords: {
      type: Number,
      default: 100
    }
  },
  data() {
    return {
      chart: null
    }
  },
  watch: {
    data: {
      handler(newData) {
        if (newData && newData.length > 0) {
          this.updateChart()
        }
      },
      deep: true
    }
  },
  mounted() {
    this.initChart()
  },
  beforeDestroy() {
    if (this.chart) {
      this.chart.dispose()
      this.chart = null
    }
  },
  methods: {
    initChart() {
      this.chart = echarts.init(this.$refs.wordCloud)
      this.updateChart()
    },
    updateChart() {
      if (!this.chart || !this.data || this.data.length === 0) {
        return
      }

      const option = {
        tooltip: {
          show: true,
          formatter: function(params) {
            return `${params.name}: ${params.value}次`
          }
        },
        series: [{
          type: 'wordCloud',
          shape: 'circle',
          left: 'center',
          top: 'center',
          width: '90%',
          height: '90%',
          right: null,
          bottom: null,
          sizeRange: [12, 60],
          rotationRange: [-45, 45],
          rotationStep: 45,
          gridSize: 8,
          drawOutOfBound: false,
          textStyle: {
            normal: {
              fontFamily: 'sans-serif',
              fontWeight: 'bold',
              color: function() {
                const colors = [
                  '#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de',
                  '#3ba272', '#fc8452', '#9a60b4', '#ea7ccc', '#ff9f7f',
                  '#32c5e9', '#67e0e3', '#9fe6b8', '#ffdb5c', '#ff9f7f'
                ]
                return colors[Math.floor(Math.random() * colors.length)]
              }
            },
            emphasis: {
              shadowBlur: 10,
              shadowColor: '#333'
            }
          },
          data: this.data.slice(0, this.maxWords).map(item => ({
            name: item.word,
            value: item.frequency
          }))
        }]
      }

      this.chart.setOption(option)

      // 添加点击事件
      this.chart.on('click', (params) => {
        if (params.componentType === 'series' && params.seriesType === 'wordCloud') {
          this.$emit('word-click', {
            word: params.name,
            frequency: params.value
          })
        }
      })
    },
    resize() {
      if (this.chart) {
        this.chart.resize()
      }
    }
  }
}
</script>

<style scoped>
.word-cloud-container {
  background: #fff;
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}
</style>
