<template>
  <div class="home">
    <el-row type="flex" justify="center">
      <el-col :xs="24" :sm="20" :md="18" :lg="16">
        <div class="header">
          <h2>农业土地与作物种植面积监测系统</h2>
          <p>先进的农业生产智能化解决方案，助力现代农业发展</p>
        </div>

        <el-divider></el-divider>

        <div class="features">
          <el-row :gutter="30">
            <el-col :sm="12">
              <div class="feature-item">
                <div class="icon">
                  <img src="@/assets/images/traj.png" alt="轨迹管理"/>
                </div>
                <h3>智能轨迹管理</h3>
                <p>
                  系统提供农机作业轨迹的实时监控和回放功能。通过先进的GPS定位和数据传输技术,管理者可以清晰掌握农机的作业路径、作业时长等关键信息,并能够通过轨迹大数据分析优化农机调度,提高作业效率。</p>
              </div>
            </el-col>
            <el-col :sm="12">
              <div class="feature-item">
                <div class="icon">
                  <img src="@/assets/images/landplot.png" alt="地块管理"/>
                </div>
                <h3>精准地块管理</h3>
                <p>
                  系统建立了完善的农田地块数据库,包括地块位置、面积、土壤性质、种植历史等多维度信息。农业管理者可对地块进行可视化管理,并根据地块属性制定精准的生产作业计划,实现分类指导、精准施策。</p>
              </div>
            </el-col>
          </el-row>
        </div>

        <el-divider></el-divider>

        <el-divider></el-divider>

        <div class="tech-specs">
          <h3>先进技术架构</h3>
          <el-row :gutter="30">
            <el-col :span="12">
              <ul>
                <li>前沿的微服务架构设计,系统灵活、可扩展性强</li>
                <li>高可用的分布式部署,确保系统稳定性</li>
                <li>移动端、Web端无缝整合,多场景应用</li>
                <li>严格的权限管控体系,多级别数据隔离</li>
              </ul>
            </el-col>
            <el-col :span="12">
              <div class="arch-img">
                <img src="@/assets/images/techframe.png" alt="系统架构"/>
              </div>
            </el-col>
          </el-row>
        </div>

        <div class="footer">
          <p>
            <b>当前版本:</b> <span>v{{ version }}</span>
          </p>
          <p>&copy; 2024 石家庄佳诚网络技术有限公司 版权所有</p>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script>
export default {
  name: 'Index',
  data() {
    return {
      version: '1.0.0'
    }
  }
}
</script>

<style lang="scss" scoped>
.home {
  padding: 30px 0;

  .header {
    text-align: center;

    h2 {
      font-size: 32px;
      color: #333;
    }

    p {
      font-size: 16px;
      color: #666;
      margin: 10px 0 0;
    }
  }

  .features {
    margin: 30px 0;

    .feature-item {
      text-align: center;
      padding: 30px;
      transition: all 0.3s;
      background-color: #f9f9f9;
      border-radius: 8px;
      height: 100%;
      display: flex;
      flex-direction: column;

      &:hover {
        box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
        transform: translateY(-5px);
      }

      .icon {
        width: 120px;
        height: 120px;
        margin: 0 auto 20px;
        overflow: hidden;
        border-radius: 50%;

        img {
          width: 100%;
          height: 100%;
          object-fit: cover;
        }
      }

      h3 {
        font-size: 22px;
        color: #333;
        margin: 20px 0;
      }

      p {
        font-size: 15px;
        line-height: 1.8;
        color: #666;
        flex-grow: 1;
      }
    }
  }

  .data-analysis {
    text-align: center;

    h3 {
      font-size: 24px;
      color: #333;
      margin-bottom: 20px;
    }

    p {
      font-size: 16px;
      line-height: 1.8;
      color: #666;
    }

    .analysis-img {
      max-width: 800px;
      margin: 30px auto 0;

      img {
        width: 100%;
      }
    }
  }

  .tech-specs {
    margin-top: 40px;

    h3 {
      font-size: 26px;
      color: #333;
      margin-bottom: 30px;
      text-align: center;
    }

    ul {
      list-style: none;
      padding: 0;

      li {
        font-size: 16px;
        line-height: 2;
        color: #666;
        margin-bottom: 10px;
        padding-left: 25px;
        position: relative;

        &:before {
          content: '✓';
          color: #4CAF50;
          position: absolute;
          left: 0;
          top: 0;
        }
      }
    }

    .arch-img {
      text-align: center;

      img {
        max-width: 100%;
        border-radius: 8px;
        box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
      }
    }
  }

  .footer {
    margin-top: 30px;
    text-align: center;
    font-size: 14px;
    color: #666;

    p {
      margin: 5px 0;
    }
  }
}

.feature-module {
  background-color: #f5f5f5;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 30px;
  transition: transform 0.3s ease;
}

.feature-module:hover {
  transform: translateY(-5px);
}

.feature-module img {
  width: 100%;
  height: auto;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}
</style>
