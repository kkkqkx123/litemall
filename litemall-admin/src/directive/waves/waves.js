export default {
  bind(el, binding) {
    el.addEventListener('click', e => {
      const customOpts = Object.assign({}, binding.value)
      const opts = Object.assign({
        ele: el,
        type: 'hit',
        color: 'rgba(0, 0, 0, 0.15)'
      }, customOpts)

      const target = opts.ele
      if (target) {
        target.style.position = 'relative'
        target.style.overflow = 'hidden'
        const rect = target.getBoundingClientRect()
        let ripple = target.querySelector('.waves-ripple')
        if (!ripple) {
          ripple = document.createElement('span')
          ripple.className = 'waves-ripple'
          ripple.style.cssText = `
            position: absolute;
            border-radius: 100%;
            background-color: ${opts.color};
            opacity: 0;
            transform: scale(0);
            pointer-events: none;
          `
          target.appendChild(ripple)
        }

        ripple.style.width = ripple.style.height = Math.max(rect.width, rect.height) + 'px'
        ripple.style.left = (e.clientX - rect.left - ripple.offsetWidth / 2) + 'px'
        ripple.style.top = (e.clientY - rect.top - ripple.offsetHeight / 2) + 'px'

        ripple.style.opacity = '1'
        ripple.style.transform = 'scale(1)'
        ripple.style.transition = 'all 0.6s ease-out'

        setTimeout(() => {
          ripple.style.opacity = '0'
          ripple.style.transform = 'scale(1.5)'
          setTimeout(() => {
            if (ripple && ripple.parentNode) {
              ripple.parentNode.removeChild(ripple)
            }
          }, 600)
        }, 200)
      }
    })
  }
}
